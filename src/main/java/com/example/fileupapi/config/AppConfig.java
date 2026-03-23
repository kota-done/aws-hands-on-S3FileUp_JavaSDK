package com.example.fileupapi.config;

public record AppConfig(String requestsTableName, String uploadBucketName) {
    public static AppConfig fromEnv() {
        return new AppConfig(
                require("REQUESTS_TABLE_NAME"),
                require("UPLOAD_BUCKET_NAME")
        );
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + name);
        }
        return value;
    }
}
