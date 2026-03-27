package com.example.fileupapi.validator;

import com.example.fileupapi.dto.CreateRequestInput;

public class RequestValidator {
    private static final String REQUEST_ID_PATTERN = "^req-[0-9a-fA-F\\-]{36}$";

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

    public void validateRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId is required");
        }
        if (!requestId.matches(REQUEST_ID_PATTERN)) {
            throw new IllegalArgumentException("requestId format is invalid");
        }
    }
}
