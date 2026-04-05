package com.example.fileupapi.service;

import com.example.fileupapi.dto.CreateRequestInput;
import com.example.fileupapi.dto.CreateRequestResponse;
import com.example.fileupapi.dto.RequestDetailResponse;
import com.example.fileupapi.dto.RequestListResponse;
import com.example.fileupapi.model.RequestRecord;
import com.example.fileupapi.model.RequestStatus;
import com.example.fileupapi.repository.RequestRepository;
import java.util.Optional;

/**
 * 受付業務処理を扱うサービス.
 *
 * 受付登録、単票取得、一覧取得を調停し、ハンドラーから業務判断を分離する。
 */
public class RequestService {
    private final RequestRepository requestRepository;
    private final S3PresignService s3PresignService;
    private final RequestIdGenerator requestIdGenerator;
    private final TimeProvider timeProvider;

    /**
     * 業務処理に必要な依存を受け取るコンストラクタ.
     *
     * @param requestRepository 受付データの永続化アクセス
     * @param s3PresignService アップロードURL生成部品
     * @param requestIdGenerator 受付ID生成部品
     * @param timeProvider 現在時刻供給部品
     */
    public RequestService(
            RequestRepository requestRepository,
            S3PresignService s3PresignService,
            RequestIdGenerator requestIdGenerator,
            TimeProvider timeProvider
    ) {
        this.requestRepository = requestRepository;
        this.s3PresignService = s3PresignService;
        this.requestIdGenerator = requestIdGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 受付を作成し、アップロードURLを含む応答を返す.
     *
     * @param input 受付作成入力
     * @return 受付作成結果
     */
    public CreateRequestResponse createRequest(CreateRequestInput input) {
        String requestId = requestIdGenerator.generate();
        String timestamp = timeProvider.now();
        String s3Key = RequestS3KeyBuilder.build(input.userId(), requestId, input.fileName());

        RequestRecord record = new RequestRecord(
                requestId,
                input.userId(),
                input.fileName(),
                s3Key,
                RequestStatus.RECEIVED,
                timestamp,
                timestamp
        );

        requestRepository.save(record);

        return new CreateRequestResponse(
                record.requestId(),
                record.userId(),
                record.fileName(),
                record.s3Key(),
                record.status().name(),
                s3PresignService.createUploadUrl(record.s3Key()),
                record.createdAt(),
                record.updatedAt()
        );
    }

    /**
     * 指定requestIdの受付を取得する.
     *
     * @param requestId 取得対象の受付ID
     * @return 受付詳細。存在しない場合は空
     */
    public Optional<RequestDetailResponse> findRequestById(String requestId) {
        return requestRepository.findById(requestId)
                .map(this::toDetailResponse);
    }

    /**
     * 受付一覧を取得する.
     *
     * @return 受付一覧応答
     */
    public RequestListResponse listRequests() {
        return new RequestListResponse(
                requestRepository.findAll().stream()
                        .map(this::toDetailResponse)
                        .toList()
        );
    }

    /**
     * 永続化モデルをAPI応答DTOへ変換する.
     *
     * @param record 受付永続化モデル
     * @return 受付詳細応答DTO
     */
    private RequestDetailResponse toDetailResponse(RequestRecord record) {
        return new RequestDetailResponse(
                record.requestId(),
                record.userId(),
                record.fileName(),
                record.s3Key(),
                record.status().name(),
                record.createdAt(),
                record.updatedAt()
        );
    }
}
