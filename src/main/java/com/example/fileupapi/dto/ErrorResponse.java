package com.example.fileupapi.dto;

/**
 * APIエラー応答DTO.
 *
 * クライアントへ返却するエラーメッセージを統一形式で表現する。
 */
public record ErrorResponse(String message) {
}
