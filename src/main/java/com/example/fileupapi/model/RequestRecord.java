package com.example.fileupapi.model;

/**
 * 受付情報の永続化モデル.
 *
 * DynamoDBへ保存する受付データの構造を表現する。
 */
public record RequestRecord(
        String requestId,
        String userId,
        String fileName,
        String s3Key,
        RequestStatus status,
        String createdAt,
        String updatedAt
) {
}
