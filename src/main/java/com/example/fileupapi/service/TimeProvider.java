package com.example.fileupapi.service;

import java.time.Instant;

public class TimeProvider {
    public String now() {
        return Instant.now().toString();
    }
}
