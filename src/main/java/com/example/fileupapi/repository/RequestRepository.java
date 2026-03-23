package com.example.fileupapi.repository;

import com.example.fileupapi.model.RequestRecord;
import com.example.fileupapi.model.RequestStatus;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RequestRepository {
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public RequestRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public void save(RequestRecord record) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(toItem(record))
                .build());
    }

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

    public List<RequestRecord> findAll() {
        ScanResponse response = dynamoDbClient.scan(ScanRequest.builder()
                .tableName(tableName)
                .build());

        return response.items().stream()
                .map(this::fromItem)
                .toList();
    }

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
