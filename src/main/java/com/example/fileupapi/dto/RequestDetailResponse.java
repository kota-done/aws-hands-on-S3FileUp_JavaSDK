package com.example.fileupapi.dto;

public record RequestDetailResponse(
        String requestId,
        String userId,
        String fileName,
        String s3Key,
        String status,
        String createdAt,
        String updatedAt
) {
}
