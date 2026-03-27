package com.example.fileupapi.service;

public final class RequestS3KeyBuilder {
    private RequestS3KeyBuilder() {
    }

    public static String build(String userId, String requestId, String fileName) {
        return "uploads/" + sanitize(userId) + "/" + sanitize(requestId) + "/" + sanitize(fileName);
    }

    private static String sanitize(String value) {
        return value.replace("\\", "_").replace("/", "_").trim();
    }
}
