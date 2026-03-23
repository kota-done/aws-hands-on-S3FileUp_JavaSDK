package com.example.fileupapi.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ObjectMapperFactory {
    private ObjectMapperFactory() {
    }

    public static ObjectMapper create() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
