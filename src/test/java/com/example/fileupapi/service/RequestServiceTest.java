package com.example.fileupapi.service;

import com.example.fileupapi.dto.CreateRequestInput;
import com.example.fileupapi.dto.CreateRequestResponse;
import com.example.fileupapi.dto.RequestDetailResponse;
import com.example.fileupapi.dto.RequestListResponse;
import com.example.fileupapi.model.RequestRecord;
import com.example.fileupapi.model.RequestStatus;
import com.example.fileupapi.repository.RequestRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestServiceTest {
    // 正常系: 受付作成時に保存とレスポンス生成が成功する場合
    // 確認項目:
    // - レスポンス項目（requestId, userId, fileName, s3Key, status, uploadUrl, createdAt, updatedAt）
    // - Repositoryへ1件保存される
    // - 保存statusが RECEIVED
    @Test
    void createRequest_CheckPersistAndResponseMapping_withValidInput() {
        InMemoryRequestRepository repository = new InMemoryRequestRepository();
        RequestService service = new RequestService(
                repository,
                new FakeS3PresignService(),
                new FixedRequestIdGenerator("req-123e4567-e89b-12d3-a456-426614174000"),
                new FixedTimeProvider("2026-03-29T00:00:00Z")
        );

        CreateRequestResponse response = service.createRequest(new CreateRequestInput("user-001", "sample.pdf"));

        assertEquals("req-123e4567-e89b-12d3-a456-426614174000", response.requestId());
        assertEquals("user-001", response.userId());
        assertEquals("sample.pdf", response.fileName());
        assertEquals("uploads/user-001/req-123e4567-e89b-12d3-a456-426614174000/sample.pdf", response.s3Key());
        assertEquals("RECEIVED", response.status());
        assertEquals("https://example.com/upload/uploads/user-001/req-123e4567-e89b-12d3-a456-426614174000/sample.pdf", response.uploadUrl());
        assertEquals("2026-03-29T00:00:00Z", response.createdAt());
        assertEquals("2026-03-29T00:00:00Z", response.updatedAt());

        assertEquals(1, repository.findAll().size());
        assertEquals(RequestStatus.RECEIVED, repository.findAll().get(0).status());
    }

    // 正常系: requestId指定で既存データを取得できる場合
    // 確認項目:
    // - Optionalが存在する
    // - DTO項目が保存データと一致する
    @Test
    void findRequestById_CheckResponseMapping_withExistingRequestId() {
        InMemoryRequestRepository repository = new InMemoryRequestRepository();
        repository.save(new RequestRecord(
                "req-123e4567-e89b-12d3-a456-426614174000",
                "user-001",
                "sample.pdf",
                "uploads/user-001/req-123e4567-e89b-12d3-a456-426614174000/sample.pdf",
                RequestStatus.RECEIVED,
                "2026-03-29T00:00:00Z",
                "2026-03-29T00:00:00Z"
        ));
        RequestService service = new RequestService(
                repository,
                new FakeS3PresignService(),
                new FixedRequestIdGenerator("unused"),
                new FixedTimeProvider("unused")
        );

        Optional<RequestDetailResponse> response = service.findRequestById("req-123e4567-e89b-12d3-a456-426614174000");

        assertTrue(response.isPresent());
        assertDetailResponse(
                response.get(),
                "req-123e4567-e89b-12d3-a456-426614174000",
                "user-001",
                "sample.pdf",
                "uploads/user-001/req-123e4567-e89b-12d3-a456-426614174000/sample.pdf",
                "RECEIVED",
                "2026-03-29T00:00:00Z",
                "2026-03-29T00:00:00Z"
        );
    }

    // 異常系: requestId指定で対象が存在しない場合
    // 確認項目:
    // - Optionalが空で返る
    @Test
    void findRequestById_CheckNotFoundHandling_withUnknownRequestId() {
        InMemoryRequestRepository repository = new InMemoryRequestRepository();
        RequestService service = new RequestService(
                repository,
                new FakeS3PresignService(),
                new FixedRequestIdGenerator("unused"),
                new FixedTimeProvider("unused")
        );

        Optional<RequestDetailResponse> response = service.findRequestById("req-123e4567-e89b-12d3-a456-426614174000");

        assertFalse(response.isPresent());
    }

    // 正常系: 複数レコードの一覧を取得できる場合
    // 確認項目:
    // - 件数が2件
    // - 各要素のDTO項目が保存データと一致する
    @Test
    void listRequests_CheckResponseMapping_withMultipleRecords() {
        InMemoryRequestRepository repository = new InMemoryRequestRepository();
        repository.save(new RequestRecord(
                "req-11111111-1111-1111-1111-111111111111",
                "user-001",
                "a.pdf",
                "uploads/user-001/req-11111111-1111-1111-1111-111111111111/a.pdf",
                RequestStatus.RECEIVED,
                "2026-03-29T00:00:00Z",
                "2026-03-29T00:00:00Z"
        ));
        repository.save(new RequestRecord(
                "req-22222222-2222-2222-2222-222222222222",
                "user-002",
                "b.pdf",
                "uploads/user-002/req-22222222-2222-2222-2222-222222222222/b.pdf",
                RequestStatus.PROCESSING,
                "2026-03-29T01:00:00Z",
                "2026-03-29T01:00:00Z"
        ));
        RequestService service = new RequestService(
                repository,
                new FakeS3PresignService(),
                new FixedRequestIdGenerator("unused"),
                new FixedTimeProvider("unused")
        );

        RequestListResponse response = service.listRequests();

        assertEquals(2, response.items().size());
        assertDetailResponse(
                response.items().get(0),
                "req-11111111-1111-1111-1111-111111111111",
                "user-001",
                "a.pdf",
                "uploads/user-001/req-11111111-1111-1111-1111-111111111111/a.pdf",
                "RECEIVED",
                "2026-03-29T00:00:00Z",
                "2026-03-29T00:00:00Z"
        );
        assertDetailResponse(
                response.items().get(1),
                "req-22222222-2222-2222-2222-222222222222",
                "user-002",
                "b.pdf",
                "uploads/user-002/req-22222222-2222-2222-2222-222222222222/b.pdf",
                "PROCESSING",
                "2026-03-29T01:00:00Z",
                "2026-03-29T01:00:00Z"
        );
    }

    private static void assertDetailResponse(
            RequestDetailResponse actual,
            String requestId,
            String userId,
            String fileName,
            String s3Key,
            String status,
            String createdAt,
            String updatedAt
    ) {
        assertEquals(requestId, actual.requestId());
        assertEquals(userId, actual.userId());
        assertEquals(fileName, actual.fileName());
        assertEquals(s3Key, actual.s3Key());
        assertEquals(status, actual.status());
        assertEquals(createdAt, actual.createdAt());
        assertEquals(updatedAt, actual.updatedAt());
    }

    private static class InMemoryRequestRepository extends RequestRepository {
        private final Map<String, RequestRecord> store = new LinkedHashMap<>();

        InMemoryRequestRepository() {
            super(null, "requests");
        }

        @Override
        public void save(RequestRecord record) {
            store.put(record.requestId(), record);
        }

        @Override
        public Optional<RequestRecord> findById(String requestId) {
            return Optional.ofNullable(store.get(requestId));
        }

        @Override
        public List<RequestRecord> findAll() {
            return new ArrayList<>(store.values());
        }
    }

    private static class FakeS3PresignService extends S3PresignService {
        FakeS3PresignService() {
            super(null, "unused-bucket");
        }

        @Override
        public String createUploadUrl(String s3Key) {
            return "https://example.com/upload/" + s3Key;
        }
    }

    private static class FixedRequestIdGenerator extends RequestIdGenerator {
        private final String requestId;

        FixedRequestIdGenerator(String requestId) {
            this.requestId = requestId;
        }

        @Override
        public String generate() {
            return requestId;
        }
    }

    private static class FixedTimeProvider extends TimeProvider {
        private final String timestamp;

        FixedTimeProvider(String timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public String now() {
            return timestamp;
        }
    }
}
