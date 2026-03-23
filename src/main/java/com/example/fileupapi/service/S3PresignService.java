package com.example.fileupapi.service;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

public class S3PresignService {
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3PresignService(S3Presigner s3Presigner, String bucketName) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    public String createUploadUrl(String s3Key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();
    }
}
