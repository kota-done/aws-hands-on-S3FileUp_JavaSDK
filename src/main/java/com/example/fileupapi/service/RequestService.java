package com.example.fileupapi.service;

import com.example.fileupapi.dto.CreateRequestInput;
import com.example.fileupapi.dto.CreateRequestResponse;
import com.example.fileupapi.dto.RequestDetailResponse;
import com.example.fileupapi.dto.RequestListResponse;
import com.example.fileupapi.model.RequestRecord;
import com.example.fileupapi.model.RequestStatus;
import com.example.fileupapi.repository.RequestRepository;
import com.example.fileupapi.util.S3KeyBuilder;

import java.util.Optional;

public class RequestService {
    private final RequestRepository requestRepository;
    private final S3PresignService s3PresignService;
    private final RequestIdGenerator requestIdGenerator;
    private final TimeProvider timeProvider;

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

    public CreateRequestResponse createRequest(CreateRequestInput input) {
        String requestId = requestIdGenerator.generate();
        String timestamp = timeProvider.now();
        String s3Key = S3KeyBuilder.build(input.userId(), requestId, input.fileName());

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

    public Optional<RequestDetailResponse> findRequestById(String requestId) {
        return requestRepository.findById(requestId)
                .map(this::toDetailResponse);
    }

    public RequestListResponse listRequests() {
        return new RequestListResponse(
                requestRepository.findAll().stream()
                        .map(this::toDetailResponse)
                        .toList()
        );
    }

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
