package com.example.fileupapi.dto;

public record CreateRequestResponse(
        String requestId,
        String userId,
        String fileName,
        String s3Key,
        String status,
        String uploadUrl,
        String createdAt,
        String updatedAt
) {
}
