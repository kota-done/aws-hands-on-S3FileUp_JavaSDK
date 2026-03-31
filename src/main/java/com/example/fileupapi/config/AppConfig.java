package com.example.fileupapi.config;

public record AppConfig(
        String requestsTableName,
        String uploadBucketName,
        String ddbEndpoint,
        String s3Endpoint,
        String ddbRegion,
        String s3Region
) {
    public static AppConfig fromEnv() {
        return new AppConfig(
                require("REQUESTS_TABLE_NAME"),
                require("UPLOAD_BUCKET_NAME"),
                optional("DDB_ENDPOINT"),
                optional("S3_ENDPOINT"),
                optional("DDB_REGION"),
                optional("S3_REGION")
        );
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + name);
        }
        return value;
    }

    private static String optional(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
