package com.example.fileupapi.service;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

/**
 * S3アップロードURL生成サービス.
 *
 * 署名付きURL生成処理を業務処理から分離し、生成ルールを一元化する。
 */
public class S3PresignService {
    private final S3Presigner s3Presigner;
    private final String bucketName;

    /**
     * S3署名生成に必要な依存を受け取るコンストラクタ.
     *
     * @param s3Presigner S3署名生成クライアント
     * @param bucketName アップロード先バケット名
     */
    public S3PresignService(S3Presigner s3Presigner, String bucketName) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    /**
     * 指定S3キーに対するPutObject用署名付きURLを生成する.
     *
     * @param s3Key アップロード対象のS3キー
     * @return 署名付きアップロードURL
     */
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
