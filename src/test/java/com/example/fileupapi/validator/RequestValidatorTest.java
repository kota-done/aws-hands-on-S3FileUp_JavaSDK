package com.example.fileupapi.validator;

import com.example.fileupapi.dto.CreateRequestInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestValidatorTest {
    private final RequestValidator validator = new RequestValidator();

    // 正常系: 受付作成入力が有効な場合
    // 確認項目:
    // - 例外が発生しない
    @Test
    void validateCreateRequest_CheckValidInput_withValidInput() {
        CreateRequestInput input = new CreateRequestInput("user-001", "sample.pdf");

        assertDoesNotThrow(() -> validator.validateCreateRequest(input));
    }

    // 異常系: リクエストボディがnullの場合
    // 確認項目:
    // - IllegalArgumentExceptionが送出される
    // - メッセージが "Request body is required"
    @Test
    void validateCreateRequest_CheckRequiredRequestBody_withNullRequest() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateCreateRequest(null)
        );

        assertEquals("Request body is required", exception.getMessage());
    }

    // 異常系: userIdが空白のみの場合
    // 確認項目:
    // - IllegalArgumentExceptionが送出される
    // - メッセージが "userId is required"
    @Test
    void validateCreateRequest_CheckRequiredUserId_withBlankUserId() {
        CreateRequestInput input = new CreateRequestInput("   ", "sample.pdf");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateCreateRequest(input)
        );

        assertEquals("userId is required", exception.getMessage());
    }

    // 異常系: userIdがnullの場合
    // 確認項目:
    // - IllegalArgumentExceptionが送出される
    // - メッセージが "userId is required"
    @Test
    void validateCreateRequest_CheckRequiredUserId_withNullUserId() {
        CreateRequestInput input = new CreateRequestInput(null, "sample.pdf");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateCreateRequest(input)
        );

        assertEquals("userId is required", exception.getMessage());
    }

    // 異常系: fileNameが空白のみの場合
    // 確認項目:
    // - IllegalArgumentExceptionが送出される
    // - メッセージが "fileName is required"
    @Test
    void validateCreateRequest_CheckRequiredFileName_withBlankFileName() {
        CreateRequestInput input = new CreateRequestInput("user-001", "   ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateCreateRequest(input)
        );

        assertEquals("fileName is required", exception.getMessage());
    }

    // 異常系: fileNameがnullの場合
    // 確認項目:
    // - IllegalArgumentExceptionが送出される
    // - メッセージが "fileName is required"
    @Test
    void validateCreateRequest_CheckRequiredFileName_withNullFileName() {
        CreateRequestInput input = new CreateRequestInput("user-001", null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateCreateRequest(input)
        );

        assertEquals("fileName is required", exception.getMessage());
    }

    // 正常系: requestId形式が有効な場合
    // 確認項目:
    // - 例外が発生しない
    @Test
    void validateRequestId_CheckFormatValidation_withValidFormat() {
        assertDoesNotThrow(() -> validator.validateRequestId("req-123e4567-e89b-12d3-a456-426614174000"));
    }

    // 異常系: requestIdが空白のみの場合
    // 確認項目:
    // - IllegalArgumentExceptionが送出される
    // - メッセージが "requestId is required"
    @Test
    void validateRequestId_CheckRequiredRequestId_withBlankRequestId() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateRequestId(" ")
        );

        assertEquals("requestId is required", exception.getMessage());
    }

    // 異常系: requestId形式が不正な場合
    // 確認項目:
    // - IllegalArgumentExceptionが送出される
    // - メッセージが "requestId format is invalid"
    @Test
    void validateRequestId_CheckFormatValidation_withInvalidFormat() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateRequestId("req-001")
        );

        assertEquals("requestId format is invalid", exception.getMessage());
    }
}
