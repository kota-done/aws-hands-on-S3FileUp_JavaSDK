package com.example.fileupapi.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.example.fileupapi.dto.CreateRequestInput;
import com.example.fileupapi.dto.CreateRequestResponse;
import com.example.fileupapi.dto.RequestDetailResponse;
import com.example.fileupapi.dto.RequestListResponse;
import com.example.fileupapi.service.RequestService;
import com.example.fileupapi.util.ObjectMapperFactory;
import com.example.fileupapi.validator.RequestValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestApiHandlerTest {
    private final ObjectMapper objectMapper = ObjectMapperFactory.create();

    // 正常系: POST /requests が成功する場合
    // 確認項目:
    // - ステータスコードが 201
    // - Content-Typeが application/json
    // - レスポンスbodyの主要項目が期待値一致
    // - Serviceへ渡すCreateRequestInputが期待値一致
    @Test
    void handleRequest_CheckCreateResponse_withPostRequestsPath() throws Exception {
        StubRequestService requestService = new StubRequestService();
        requestService.createResponse = new CreateRequestResponse(
                "req-11111111-1111-1111-1111-111111111111",
                "user-001",
                "sample.txt",
                "requests/user-001/req-11111111-1111-1111-1111-111111111111/sample.txt",
                "RECEIVED",
                "https://example.com/upload",
                "2026-01-01T00:00:00Z",
                "2026-01-01T00:00:00Z"
        );
        RequestApiHandler handler = new RequestApiHandler(objectMapper, requestService, new RequestValidator());

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withPath("/requests")
                .withBody("{\"userId\":\"user-001\",\"fileName\":\"sample.txt\"}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(201, response.getStatusCode());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("req-11111111-1111-1111-1111-111111111111", body.get("requestId").asText());
        assertEquals("user-001", body.get("userId").asText());
        assertEquals("sample.txt", body.get("fileName").asText());
        assertEquals("user-001", requestService.lastCreateInput.userId());
        assertEquals("sample.txt", requestService.lastCreateInput.fileName());
    }

    // 正常系: GET /requests/{id} で pathParameters.id を優先して取得できる場合
    // 確認項目:
    // - ステータスコードが 200
    // - レスポンスbodyのrequestId, userIdが期待値一致
    // - Service呼び出しrequestIdが pathParameters.id の値
    @Test
    void handleRequest_CheckRequestDetailResponse_withGetRequestIdPathAndPathParameterId() throws Exception {
        StubRequestService requestService = new StubRequestService();
        requestService.findByIdResponse = Optional.of(new RequestDetailResponse(
                "req-11111111-1111-1111-1111-111111111111",
                "user-001",
                "sample.txt",
                "requests/user-001/req-11111111-1111-1111-1111-111111111111/sample.txt",
                "RECEIVED",
                "2026-01-01T00:00:00Z",
                "2026-01-01T00:00:00Z"
        ));
        RequestApiHandler handler = new RequestApiHandler(objectMapper, requestService, new RequestValidator());

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withHttpMethod("GET")
                .withPath("/requests/path-id-will-be-ignored")
                .withPathParameters(Map.of("id", "req-11111111-1111-1111-1111-111111111111"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(200, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("req-11111111-1111-1111-1111-111111111111", body.get("requestId").asText());
        assertEquals("user-001", body.get("userId").asText());
        assertEquals("req-11111111-1111-1111-1111-111111111111", requestService.lastFindByIdRequestId);
    }

    // 異常系: GET /requests/{id} で対象が存在しない場合
    // 確認項目:
    // - ステータスコードが 404
    // - エラーメッセージが "Request not found"
    @Test
    void handleRequest_CheckNotFoundResponse_withGetRequestIdPathAndMissingRecord() throws Exception {
        StubRequestService requestService = new StubRequestService();
        requestService.findByIdResponse = Optional.empty();
        RequestApiHandler handler = new RequestApiHandler(objectMapper, requestService, new RequestValidator());

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withHttpMethod("GET")
                .withPath("/requests/req-11111111-1111-1111-1111-111111111111");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(404, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Request not found", body.get("message").asText());
    }

    // 正常系: GET /requests/{id} で pathParameters未指定時にpathからフォールバック抽出できる場合
    // 確認項目:
    // - ステータスコードが 200
    // - Service呼び出しrequestIdがpath末尾から抽出される
    @Test
    void handleRequest_CheckRequestDetailResponse_withGetRequestIdPathAndFallbackFromPath() throws Exception {
        StubRequestService requestService = new StubRequestService();
        requestService.findByIdResponse = Optional.of(new RequestDetailResponse(
                "req-11111111-1111-1111-1111-111111111111",
                "user-001",
                "sample.txt",
                "requests/user-001/req-11111111-1111-1111-1111-111111111111/sample.txt",
                "RECEIVED",
                "2026-01-01T00:00:00Z",
                "2026-01-01T00:00:00Z"
        ));
        RequestApiHandler handler = new RequestApiHandler(objectMapper, requestService, new RequestValidator());

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withHttpMethod("GET")
                .withPath("/requests/req-11111111-1111-1111-1111-111111111111");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(200, response.getStatusCode());
        assertEquals("req-11111111-1111-1111-1111-111111111111", requestService.lastFindByIdRequestId);
    }

    // 境界系: GET /requests/{id} で pathParameters.id の形式が不正な場合
    // 確認項目:
    // - ステータスコードが 400
    // - エラーメッセージが "requestId format is invalid"
    @Test
    void handleRequest_CheckBadRequestResponse_withGetRequestIdPathAndInvalidPathParameterId() throws Exception {
        StubRequestService requestService = new StubRequestService();
        RequestApiHandler handler = new RequestApiHandler(objectMapper, requestService, new RequestValidator());

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withHttpMethod("GET")
                .withPath("/requests/req-11111111-1111-1111-1111-111111111111")
                .withPathParameters(Map.of("id", "invalid-id"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("requestId format is invalid", body.get("message").asText());
    }

    // 異常系: POST /requests の入力バリデーションに失敗する場合
    // 確認項目:
    // - ステータスコードが 400
    // - エラーメッセージが "userId is required"
    @Test
    void handleRequest_CheckBadRequestResponse_withInvalidCreateRequestBody() throws Exception {
        StubRequestService requestService = new StubRequestService();
        RequestApiHandler handler = new RequestApiHandler(objectMapper, requestService, new RequestValidator());

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withPath("/requests")
                .withBody("{\"userId\":\"\",\"fileName\":\"sample.txt\"}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("userId is required", body.get("message").asText());
    }

    // 境界系: 未定義ルートの場合
    // 確認項目:
    // - ステータスコードが 404
    // - エラーメッセージが "Route not found"
    @Test
    void handleRequest_CheckNotFoundResponse_withUnknownRoute() throws Exception {
        StubRequestService requestService = new StubRequestService();
        RequestApiHandler handler = new RequestApiHandler(objectMapper, requestService, new RequestValidator());

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withHttpMethod("PATCH")
                .withPath("/requests");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(404, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Route not found", body.get("message").asText());
    }

    // 境界系: /requests/ (末尾スラッシュ付き) は非対応ルートとして扱う場合
    // 確認項目:
    // - ステータスコードが 404
    // - エラーメッセージが "Route not found"
    @Test
    void handleRequest_CheckNotFoundResponse_withRequestsPathTrailingSlash() throws Exception {
        StubRequestService requestService = new StubRequestService();
        RequestApiHandler handler = new RequestApiHandler(objectMapper, requestService, new RequestValidator());

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withHttpMethod("GET")
                .withPath("/requests/");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(404, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Route not found", body.get("message").asText());
    }

    // 異常系: 一覧取得処理でService例外が発生する場合
    // 確認項目:
    // - ステータスコードが 500
    // - エラーメッセージが "Internal server error"
    @Test
    void handleRequest_CheckInternalServerErrorResponse_withServiceExceptionOnList() throws Exception {
        StubRequestService requestService = new StubRequestService();
        requestService.throwOnList = true;
        RequestApiHandler handler = new RequestApiHandler(objectMapper, requestService, new RequestValidator());

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withHttpMethod("GET")
                .withPath("/requests");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(500, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("Internal server error", body.get("message").asText());
    }

    private static final class StubRequestService extends RequestService {
        private CreateRequestInput lastCreateInput;
        private String lastFindByIdRequestId;
        private CreateRequestResponse createResponse;
        private Optional<RequestDetailResponse> findByIdResponse = Optional.empty();
        private boolean throwOnList;

        private StubRequestService() {
            super(null, null, null, null);
        }

        @Override
        public CreateRequestResponse createRequest(CreateRequestInput input) {
            this.lastCreateInput = input;
            if (createResponse == null) {
                throw new IllegalStateException("createResponse is not set");
            }
            return createResponse;
        }

        @Override
        public Optional<RequestDetailResponse> findRequestById(String requestId) {
            this.lastFindByIdRequestId = requestId;
            return findByIdResponse;
        }

        @Override
        public RequestListResponse listRequests() {
            if (throwOnList) {
                throw new RuntimeException("list failed");
            }
            return new RequestListResponse(List.of());
        }
    }
}
