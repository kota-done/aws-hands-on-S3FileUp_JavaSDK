package com.example.fileupapi.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ObjectMapper生成ユーティリティ.
 *
 * API入力の許容方針を統一し、ハンドラー間で同じ設定を再利用するために使用する。
 */
public final class ObjectMapperFactory {
    /**
     * インスタンス化を禁止する.
     */
    private ObjectMapperFactory() {
    }

    /**
     * アプリケーション既定設定のObjectMapperを生成する.
     *
     * @return 未知プロパティを許容するObjectMapper
     */
    public static ObjectMapper create() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
