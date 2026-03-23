package com.example.fileupapi.util;

public final class S3KeyBuilder {
    private S3KeyBuilder() {
    }

    public static String build(String userId, String requestId, String fileName) {
        return "uploads/" + sanitize(userId) + "/" + sanitize(requestId) + "/" + sanitize(fileName);
    }

    private static String sanitize(String value) {
        return value.replace("\\", "_").replace("/", "_").trim();
    }
}
