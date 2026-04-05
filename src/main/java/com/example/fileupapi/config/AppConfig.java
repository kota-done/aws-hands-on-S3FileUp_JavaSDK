package com.example.fileupapi.config;

/**
 * 環境変数から読み取るアプリケーション設定.
 *
 * 実行環境差分を吸収し、各ハンドラーやサービスの初期化を一元化するために使用する。
 */
public record AppConfig(
        String requestsTableName,
        String uploadBucketName,
        String ddbEndpoint,
        String s3Endpoint,
        String ddbRegion,
        String s3Region
) {
    /**
     * 環境変数から設定値を読み取り、AppConfigを生成する.
     *
     * @return 読み取った設定値を保持するAppConfig
     */
    public static AppConfig fromEnv() {
        return new AppConfig(
                require("REQUESTS_TABLE_NAME"),
                require("UPLOAD_BUCKET_NAME"),
                optional("DDB_ENDPOINT"),
                optional("S3_ENDPOINT"),
                optional("DDB_REGION"),
                optional("S3_REGION")
        );
    }

    /**
     * 必須設定値を読み取る.
     *
     * @param name 環境変数名
     * @return 環境変数の値
     * @throws IllegalStateException 未設定または空文字の場合
     */
    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + name);
        }
        return value;
    }

    /**
     * 任意設定値を読み取る.
     *
     * @param name 環境変数名
     * @return 環境変数の値。未設定または空文字の場合はnull
     */
    private static String optional(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
