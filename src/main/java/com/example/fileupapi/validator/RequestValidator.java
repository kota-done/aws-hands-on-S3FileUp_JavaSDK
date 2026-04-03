package com.example.fileupapi.validator;

import com.example.fileupapi.dto.CreateRequestInput;

/**
 * 受付API入力検証部品.
 *
 * ハンドラーから検証責務を分離し、入力不備を一貫した例外で返すために使用する。
 */
public class RequestValidator {
    private static final String REQUEST_ID_PATTERN = "^req-[0-9a-fA-F\\-]{36}$";

    /**
     * 受付作成入力を検証する.
     *
     * @param request 受付作成入力
     * @throws IllegalArgumentException 必須値不足または空文字の場合
     */
    public void validateCreateRequest(CreateRequestInput request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.userId() == null || request.userId().isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.fileName() == null || request.fileName().isBlank()) {
            throw new IllegalArgumentException("fileName is required");
        }
    }

    /**
     * requestId入力を検証する.
     *
     * @param requestId 受付ID
     * @throws IllegalArgumentException 未指定または形式不正の場合
     */
    public void validateRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId is required");
        }
        if (!requestId.matches(REQUEST_ID_PATTERN)) {
            throw new IllegalArgumentException("requestId format is invalid");
        }
    }
}
