package com.example.fileupapi.model;

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
