package com.example.fileupapi.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.example.fileupapi.config.AppConfig;
import com.example.fileupapi.dto.CreateRequestInput;
import com.example.fileupapi.dto.ErrorResponse;
import com.example.fileupapi.repository.RequestRepository;
import com.example.fileupapi.service.RequestIdGenerator;
import com.example.fileupapi.service.RequestService;
import com.example.fileupapi.service.S3PresignService;
import com.example.fileupapi.service.TimeProvider;
import com.example.fileupapi.util.ApiResponseBuilder;
import com.example.fileupapi.util.ObjectMapperFactory;
import com.example.fileupapi.validator.RequestValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.util.Map;

public class RequestApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final ObjectMapper objectMapper;
    private final RequestService requestService;
    private final RequestValidator requestValidator;

    public RequestApiHandler() {
        this(ObjectMapperFactory.create(), AppConfig.fromEnv());
    }

    RequestApiHandler(ObjectMapper objectMapper, AppConfig config) {
        this.objectMapper = objectMapper;
        DynamoDbClient dynamoDbClient = createDynamoDbClient(config);
        S3Presigner s3Presigner = createS3Presigner(config);

        RequestRepository requestRepository = new RequestRepository(dynamoDbClient, config.requestsTableName());
        S3PresignService s3PresignService = new S3PresignService(s3Presigner, config.uploadBucketName());
        this.requestService = new RequestService(
                requestRepository,
                s3PresignService,
                new RequestIdGenerator(),
                new TimeProvider()
        );
        this.requestValidator = new RequestValidator();
    }

    RequestApiHandler(ObjectMapper objectMapper, RequestService requestService, RequestValidator requestValidator) {
        this.objectMapper = objectMapper;
        this.requestService = requestService;
        this.requestValidator = requestValidator;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            String method = input.getHttpMethod();
            String path = input.getPath();

            if ("POST".equalsIgnoreCase(method) && "/requests".equals(path)) {
                CreateRequestInput request = objectMapper.readValue(input.getBody(), CreateRequestInput.class);
                requestValidator.validateCreateRequest(request);
                return ApiResponseBuilder.json(201, objectMapper.writeValueAsString(requestService.createRequest(request)));
            }

            if ("GET".equalsIgnoreCase(method) && path != null && path.matches("^/requests/[^/]+$")) {
                String requestId = extractRequestId(input);
                requestValidator.validateRequestId(requestId);
                return requestService.findRequestById(requestId)
                        .map(detail -> ApiResponseBuilder.json(200, writeJson(detail)))
                        .orElseGet(() -> ApiResponseBuilder.json(404, writeJson(new ErrorResponse("Request not found"))));
            }

            if ("GET".equalsIgnoreCase(method) && "/requests".equals(path)) {
                return ApiResponseBuilder.json(200, writeJson(requestService.listRequests()));
            }

            return ApiResponseBuilder.json(404, writeJson(new ErrorResponse("Route not found")));
        } catch (IllegalArgumentException e) {
            return ApiResponseBuilder.json(400, writeJson(new ErrorResponse(e.getMessage())));
        } catch (Exception e) {
            if (context != null && context.getLogger() != null) {
                context.getLogger().log("Unhandled error: " + e.getMessage());
            }
            return ApiResponseBuilder.json(500, writeJson(new ErrorResponse("Internal server error")));
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize response", e);
        }
    }

    private String extractRequestId(APIGatewayProxyRequestEvent input) {
        Map<String, String> pathParameters = input.getPathParameters();
        if (pathParameters != null) {
            String requestId = pathParameters.get("id");
            if (requestId != null) {
                return requestId;
            }
        }
        String path = input.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private DynamoDbClient createDynamoDbClient(AppConfig config) {
        if (config.ddbEndpoint() == null) {
            return DynamoDbClient.create();
        }
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(config.ddbEndpoint()))
                .region(resolveRegion(config.ddbRegion(), "us-east-1"))
                .credentialsProvider(resolveCredentialsProvider())
                .build();
    }

    private S3Presigner createS3Presigner(AppConfig config) {
        if (config.s3Endpoint() == null) {
            return S3Presigner.create();
        }
        return S3Presigner.builder()
                .endpointOverride(URI.create(config.s3Endpoint()))
                .region(resolveRegion(config.s3Region(), "us-east-1"))
                .credentialsProvider(resolveCredentialsProvider())
                .build();
    }

    private Region resolveRegion(String explicitRegion, String defaultRegion) {
        if (explicitRegion != null && !explicitRegion.isBlank()) {
            return Region.of(explicitRegion);
        }
        String region = System.getenv("AWS_REGION");
        if (region == null || region.isBlank()) {
            region = System.getenv("AWS_DEFAULT_REGION");
        }
        if (region == null || region.isBlank()) {
            region = defaultRegion;
        }
        return Region.of(region);
    }

    private StaticCredentialsProvider resolveCredentialsProvider() {
        String accessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
        if (accessKeyId == null || accessKeyId.isBlank()) {
            accessKeyId = "test";
        }
        String secretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        if (secretAccessKey == null || secretAccessKey.isBlank()) {
            secretAccessKey = "test";
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
    }
}
