package com.example.fileupapi.repository;

import com.example.fileupapi.model.RequestRecord;
import com.example.fileupapi.model.RequestStatus;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 受付情報のDynamoDBアクセス部品.
 *
 * 永続化I/Oを一箇所へ集約し、サービス層からAWS SDK詳細を分離するために使用する。
 */
public class RequestRepository {
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    /**
     * DynamoDBアクセス依存を受け取るコンストラクタ.
     *
     * @param dynamoDbClient DynamoDBクライアント
     * @param tableName 受付テーブル名
     */
    public RequestRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    /**
     * 受付情報を保存する.
     *
     * @param record 保存対象の受付情報
     */
    public void save(RequestRecord record) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(toItem(record))
                .build());
    }

    /**
     * requestIdで受付情報を取得する.
     *
     * @param requestId 取得対象の受付ID
     * @return 取得結果。存在しない場合は空
     */
    public Optional<RequestRecord> findById(String requestId) {
        Map<String, AttributeValue> key = Map.of(
                "requestId", AttributeValue.builder().s(requestId).build()
        );

        Map<String, AttributeValue> item = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build()).item();

        if (item == null || item.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fromItem(item));
    }

    /**
     * 受付一覧を取得する.
     *
     * @return 受付情報一覧
     */
    public List<RequestRecord> findAll() {
        ScanResponse response = dynamoDbClient.scan(ScanRequest.builder()
                .tableName(tableName)
                .build());

        return response.items().stream()
                .map(this::fromItem)
                .toList();
    }

    /**
     * 指定requestIdの状態を更新する.
     *
     * @param requestId 更新対象の受付ID
     * @param status 更新後の状態
     * @param updatedAt 更新時刻
     * @return 更新成功時はtrue。対象が存在しない場合はfalse
     */
    public boolean updateStatus(String requestId, RequestStatus status, String updatedAt) {
        Map<String, AttributeValue> key = Map.of(
                "requestId", AttributeValue.builder().s(requestId).build()
        );

        Map<String, String> names = Map.of(
                "#status", "status",
                "#updatedAt", "updatedAt"
        );
        Map<String, AttributeValue> values = Map.of(
                ":status", AttributeValue.builder().s(status.name()).build(),
                ":updatedAt", AttributeValue.builder().s(updatedAt).build()
        );

        try {
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .conditionExpression("attribute_exists(requestId)")
                    .updateExpression("SET #status = :status, #updatedAt = :updatedAt")
                    .expressionAttributeNames(names)
                    .expressionAttributeValues(values)
                    .build());
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    /**
     * 永続化モデルをDynamoDB Itemへ変換する.
     *
     * @param record 変換対象の受付情報
     * @return DynamoDB保存用Item
     */
    private Map<String, AttributeValue> toItem(RequestRecord record) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("requestId", AttributeValue.builder().s(record.requestId()).build());
        item.put("userId", AttributeValue.builder().s(record.userId()).build());
        item.put("fileName", AttributeValue.builder().s(record.fileName()).build());
        item.put("s3Key", AttributeValue.builder().s(record.s3Key()).build());
        item.put("status", AttributeValue.builder().s(record.status().name()).build());
        item.put("createdAt", AttributeValue.builder().s(record.createdAt()).build());
        item.put("updatedAt", AttributeValue.builder().s(record.updatedAt()).build());
        return item;
    }

    /**
     * DynamoDB Itemを永続化モデルへ変換する.
     *
     * @param item DynamoDB取得Item
     * @return 受付永続化モデル
     */
    private RequestRecord fromItem(Map<String, AttributeValue> item) {
        return new RequestRecord(
                item.get("requestId").s(),
                item.get("userId").s(),
                item.get("fileName").s(),
                item.get("s3Key").s(),
                RequestStatus.valueOf(item.get("status").s()),
                item.get("createdAt").s(),
                item.get("updatedAt").s()
        );
    }
}
