package com.example.fileupapi.service;

import java.util.UUID;

public class RequestIdGenerator {
    public String generate() {
        return "req-" + UUID.randomUUID();
    }
}
