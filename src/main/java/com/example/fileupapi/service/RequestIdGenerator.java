package com.example.fileupapi.service;

import java.util.UUID;

/**
 * 受付ID生成部品.
 *
 * requestId採番ロジックをService本体から分離し、テストしやすさを保つ。
 */
public class RequestIdGenerator {
    /**
     * 一意なrequestIdを生成する.
     *
     * @return req-接頭辞付きの受付ID
     */
    public String generate() {
        return "req-" + UUID.randomUUID();
    }
}
