package com.example.Voucher.dto;

import java.time.Instant;

public class ApiError {
    private final int status;
    private final String error;
    private final String message;
    private final Instant timestamp;

    public ApiError(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }
}
