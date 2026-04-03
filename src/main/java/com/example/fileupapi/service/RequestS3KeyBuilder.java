package com.example.fileupapi.service;

/**
 * 受付API用S3キー生成部品.
 *
 * 保存先キーの規約を一箇所に集約し、ハンドラーやサービスの責務を単純化する。
 */
public final class RequestS3KeyBuilder {
    /**
     * インスタンス化を禁止する.
     */
    private RequestS3KeyBuilder() {
    }

    /**
     * S3保存先キーを生成する.
     *
     * @param userId 利用者ID
     * @param requestId 受付ID
     * @param fileName 元ファイル名
     * @return uploads/{userId}/{requestId}/{fileName}形式のS3キー
     */
    public static String build(String userId, String requestId, String fileName) {
        return "uploads/" + sanitize(userId) + "/" + sanitize(requestId) + "/" + sanitize(fileName);
    }

    /**
     * S3キーとして不適切な文字を置換する.
     *
     * @param value 変換対象文字列
     * @return 区切り文字を置換した文字列
     */
    private static String sanitize(String value) {
        return value.replace("\\", "_").replace("/", "_").trim();
    }
}
