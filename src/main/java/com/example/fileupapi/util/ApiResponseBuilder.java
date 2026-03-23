package com.example.fileupapi.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.Map;

public final class ApiResponseBuilder {
    private ApiResponseBuilder() {
    }

    public static APIGatewayProxyResponseEvent json(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(body);
    }
}
