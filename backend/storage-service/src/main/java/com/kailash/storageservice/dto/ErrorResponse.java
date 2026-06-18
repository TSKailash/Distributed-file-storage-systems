package com.kailash.storageservice.dto;

import java.time.LocalDateTime;

public class ErrorResponse {
    public LocalDateTime timestamp;
    private int status;
    private String message;

    public ErrorResponse(int status, String message){
        this.timestamp=LocalDateTime.now();
        this.status=status;
        this.message=message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
