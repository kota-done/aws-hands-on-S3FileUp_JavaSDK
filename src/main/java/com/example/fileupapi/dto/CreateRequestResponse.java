package com.example.fileupapi.dto;

/**
 * 受付作成APIの出力DTO.
 *
 * 受付情報とアップロードURLを返却し、クライアントの次操作を決定できるようにする。
 */
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
