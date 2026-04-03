package com.example.fileupapi.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.Map;

/**
 * API Gatewayレスポンス生成ユーティリティ.
 *
 * ヘッダーを含むJSON応答の組み立てを共通化し、ハンドラー記述を簡潔に保つ。
 */
public final class ApiResponseBuilder {
    /**
     * インスタンス化を禁止する.
     */
    private ApiResponseBuilder() {
    }

    /**
     * JSONレスポンスを生成する.
     *
     * @param statusCode HTTPステータスコード
     * @param body JSON文字列
     * @return API Gatewayレスポンス
     */
    public static APIGatewayProxyResponseEvent json(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(body);
    }
}
