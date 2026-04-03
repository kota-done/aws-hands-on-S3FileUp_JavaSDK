package com.example.fileupapi.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestS3KeyBuilderTest {
    // 正常系: 規約どおりのS3キーを生成できる場合
    // 確認項目:
    // - 生成結果が uploads/{userId}/{requestId}/{fileName} 形式
    // - 入力値がそのまま連結される
    @Test
    void build_CheckPathFormat_withNormalValues() {
        String result = RequestS3KeyBuilder.build("user-001", "req-123", "sample.pdf");

        assertEquals("uploads/user-001/req-123/sample.pdf", result);
    }

    // 境界系: スラッシュやバックスラッシュ、前後空白を含む場合
    // 確認項目:
    // - "/" と "\" が "_" に置換される
    // - 前後空白が除去される
    // - 置換後のキーが規約形式で返る
    @Test
    void build_CheckSanitizeRules_withSlashAndBackslashAndSpaces() {
        String result = RequestS3KeyBuilder.build(" user/a\\b ", " req/001 ", " path\\to/file.txt ");

        assertEquals("uploads/user_a_b/req_001/path_to_file.txt", result);
    }
}
