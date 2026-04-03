package com.example.fileupapi.dto;

/**
 * 受付単票取得APIの出力DTO.
 *
 * 指定requestIdの状態確認に必要な項目を返す。
 */
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
