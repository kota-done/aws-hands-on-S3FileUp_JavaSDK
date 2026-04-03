package com.example.fileupapi.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.fileupapi.config.AppConfig;
import com.example.fileupapi.model.RequestStatus;
import com.example.fileupapi.repository.RequestRepository;
import com.example.fileupapi.service.TimeProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * S3のObject Createdイベントを受け取り、受付状態を更新するハンドラー.
 *
 * EventBridge経由で通知されたイベントを処理し、受付状態反映をAPI処理から分離するために使用する。
 */
public class RequestStatusUpdateHandler implements RequestHandler<Map<String, Object>, String> {
    private final RequestRepository requestRepository;
    private final TimeProvider timeProvider;

    /**
     * Lambdaランタイム用のデフォルトコンストラクタ.
     *
     * 環境変数から設定を読み取り、本番実行に必要な依存を組み立てる。
     */
    public RequestStatusUpdateHandler() {
        this(AppConfig.fromEnv());
    }

    /**
     * 設定値をもとに依存を組み立てるコンストラクタ.
     *
     * @param config 環境変数から解決したアプリケーション設定
     */
    RequestStatusUpdateHandler(AppConfig config) {
        this(
                new RequestRepository(createDynamoDbClient(config), config.requestsTableName()),
                new TimeProvider()
        );
    }

    /**
     * テストや差し替えのために依存を注入するコンストラクタ.
     *
     * @param requestRepository 状態更新対象の永続化アクセス
     * @param timeProvider 更新時刻を供給する部品
     */
    RequestStatusUpdateHandler(RequestRepository requestRepository, TimeProvider timeProvider) {
        this.requestRepository = requestRepository;
        this.timeProvider = timeProvider;
    }

    /**
     * EventBridgeイベントを処理し、対象受付の状態を更新する.
     *
     * @param input Lambdaに渡されるイベントペイロード
     * @param context Lambda実行コンテキスト
     * @return 更新成功時は"UPDATED"、対象外または更新不可の場合は"IGNORED"
     */
    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        String objectKey = extractObjectKey(input);
        if (objectKey == null || objectKey.isBlank()) {
            log(context, "Ignored event: object key is missing");
            return "IGNORED";
        }

        String requestId = extractRequestId(objectKey);
        if (requestId == null) {
            log(context, "Ignored event: requestId not found in key=" + objectKey);
            return "IGNORED";
        }

        boolean updated = requestRepository.updateStatus(requestId, RequestStatus.COMPLETED, timeProvider.now());
        if (!updated) {
            log(context, "Ignored event: request record not found. requestId=" + requestId);
            return "IGNORED";
        }
        return "UPDATED";
    }

    /**
     * EventBridgeイベントからS3オブジェクトキーを抽出する.
     *
     * detail.object.keyの構造に一致しない場合はnullを返し、後段で対象外イベントとして扱う。
     *
     * @param input Lambdaに渡されるイベントペイロード
     * @return URLデコード後のS3キー。抽出できない場合はnull
     */
    private String extractObjectKey(Map<String, Object> input) {
        Object detailObject = input.get("detail");
        if (!(detailObject instanceof Map<?, ?> detail)) {
            return null;
        }
        Object objectObject = detail.get("object");
        if (!(objectObject instanceof Map<?, ?> object)) {
            return null;
        }
        Object keyObject = object.get("key");
        if (!(keyObject instanceof String key)) {
            return null;
        }
        return URLDecoder.decode(key, StandardCharsets.UTF_8);
    }

    /**
     * S3キーから受付IDを抽出する.
     *
     * uploads/{userId}/{requestId}/{fileName}形式に一致しない場合はnullを返す。
     *
     * @param objectKey S3オブジェクトキー
     * @return 抽出したrequestId。抽出できない場合はnull
     */
    private String extractRequestId(String objectKey) {
        String[] parts = objectKey.split("/");
        if (parts.length < 4) {
            return null;
        }
        if (!"uploads".equals(parts[0])) {
            return null;
        }
        String requestId = parts[2];
        return requestId.isBlank() ? null : requestId;
    }

    /**
     * 設定に応じてDynamoDBクライアントを生成する.
     *
     * ローカル検証ではエンドポイント上書きを使い、未指定時はAWS標準設定を利用する。
     *
     * @param config 環境変数から解決したアプリケーション設定
     * @return 生成したDynamoDBクライアント
     */
    private static DynamoDbClient createDynamoDbClient(AppConfig config) {
        if (config.ddbEndpoint() == null) {
            return DynamoDbClient.create();
        }
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(config.ddbEndpoint()))
                .region(resolveRegion(config.ddbRegion(), "us-east-1"))
                .credentialsProvider(resolveCredentialsProvider())
                .build();
    }

    /**
     * 利用リージョンを解決する.
     *
     * 明示設定、Lambda標準環境変数、既定値の順で評価して起動環境差分を吸収する。
     *
     * @param explicitRegion 設定で明示されたリージョン
     * @param defaultRegion フォールバック時に使う既定リージョン
     * @return 解決したリージョン
     */
    private static Region resolveRegion(String explicitRegion, String defaultRegion) {
        if (explicitRegion != null && !explicitRegion.isBlank()) {
            return Region.of(explicitRegion);
        }
        String region = System.getenv("AWS_REGION");
        if (region == null || region.isBlank()) {
            region = System.getenv("AWS_DEFAULT_REGION");
        }
        if (region == null || region.isBlank()) {
            region = defaultRegion;
        }
        return Region.of(region);
    }

    /**
     * 認証情報プロバイダを解決する.
     *
     * ローカル検証で未設定でも動作できるよう、最小のダミー資格情報へフォールバックする。
     *
     * @return 解決した資格情報プロバイダ
     */
    private static StaticCredentialsProvider resolveCredentialsProvider() {
        String accessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
        if (accessKeyId == null || accessKeyId.isBlank()) {
            accessKeyId = "test";
        }
        String secretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        if (secretAccessKey == null || secretAccessKey.isBlank()) {
            secretAccessKey = "test";
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
    }

    /**
     * Lambdaコンテキストロガーへメッセージを出力する.
     *
     * @param context Lambda実行コンテキスト
     * @param message 出力するログメッセージ
     */
    private void log(Context context, String message) {
        if (context != null && context.getLogger() != null) {
            context.getLogger().log(message);
        }
    }
}
