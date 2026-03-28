package com.example.fileupapi.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestS3KeyBuilderTest {
    @Test
    void build_CheckPathFormat_withNormalValues() {
        String result = RequestS3KeyBuilder.build("user-001", "req-123", "sample.pdf");

        assertEquals("uploads/user-001/req-123/sample.pdf", result);
    }

    @Test
    void build_CheckSanitizeRules_withSlashAndBackslashAndSpaces() {
        String result = RequestS3KeyBuilder.build(" user/a\\b ", " req/001 ", " path\\to/file.txt ");

        assertEquals("uploads/user_a_b/req_001/path_to_file.txt", result);
    }
}
