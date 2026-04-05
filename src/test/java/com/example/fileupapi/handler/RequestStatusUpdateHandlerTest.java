package com.example.fileupapi.handler;

import com.example.fileupapi.model.RequestStatus;
import com.example.fileupapi.repository.RequestRepository;
import com.example.fileupapi.service.TimeProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestStatusUpdateHandlerTest {
    // 正常系: S3イベントで状態更新できること
    // 確認項目:
    // - 戻り値が "UPDATED"
    // - 抽出requestIdが "req-123"
    // - 更新statusが COMPLETED
    // - 更新updatedAtが固定時刻 "2026-04-04T00:00:00Z"
    @Test
    void handleRequest_CheckCompletedUpdate_withS3ObjectCreatedEvent() {
        StubRequestRepository repository = new StubRequestRepository();
        repository.updateResult = true;
        RequestStatusUpdateHandler handler = new RequestStatusUpdateHandler(repository, new FixedTimeProvider());

        String result = handler.handleRequest(eventWithObjectKey("uploads/u001/req-123/a.txt"), null);

        assertEquals("UPDATED", result);
        assertEquals("req-123", repository.lastRequestId);
        assertEquals(RequestStatus.COMPLETED, repository.lastStatus);
        assertEquals("2026-04-04T00:00:00Z", repository.lastUpdatedAt);
    }

    // 境界系: オブジェクトキー形式が不正な場合
    // 確認項目:
    // - 戻り値が "IGNORED"
    // - requestIdが抽出されず null のまま
    // - 更新処理が実質実行されない（Repository呼び出し条件を満たさない）
    @Test
    void handleRequest_CheckIgnoredResult_withInvalidObjectKey() {
        StubRequestRepository repository = new StubRequestRepository();
        RequestStatusUpdateHandler handler = new RequestStatusUpdateHandler(repository, new FixedTimeProvider());

        String result = handler.handleRequest(eventWithObjectKey("invalid-key-format"), null);

        assertEquals("IGNORED", result);
        assertNull(repository.lastRequestId);
    }

    // 異常系: requestIdは抽出できるが更新対象が存在しない場合
    // 確認項目:
    // - 戻り値が "IGNORED"
    // - 抽出requestIdが "req-999"
    // - 更新結果false時に例外化せず終了する
    @Test
    void handleRequest_CheckIgnoredResult_withMissingRequestRecord() {
        StubRequestRepository repository = new StubRequestRepository();
        repository.updateResult = false;
        RequestStatusUpdateHandler handler = new RequestStatusUpdateHandler(repository, new FixedTimeProvider());

        String result = handler.handleRequest(eventWithObjectKey("uploads/u001/req-999/a.txt"), null);

        assertEquals("IGNORED", result);
        assertEquals("req-999", repository.lastRequestId);
    }

    private static Map<String, Object> eventWithObjectKey(String objectKey) {
        return Map.of(
                "detail", Map.of(
                        "object", Map.of("key", objectKey)
                )
        );
    }

    private static final class FixedTimeProvider extends TimeProvider {
        @Override
        public String now() {
            return "2026-04-04T00:00:00Z";
        }
    }

    private static final class StubRequestRepository extends RequestRepository {
        private String lastRequestId;
        private RequestStatus lastStatus;
        private String lastUpdatedAt;
        private boolean updateResult;

        private StubRequestRepository() {
            super(null, "unused");
        }

        @Override
        public boolean updateStatus(String requestId, RequestStatus status, String updatedAt) {
            this.lastRequestId = requestId;
            this.lastStatus = status;
            this.lastUpdatedAt = updatedAt;
            return updateResult;
        }
    }
}
