package com.skillbridge.dto.contact.request;

/**
 * Add Log Request DTO
 * Request DTO for adding a communication log
 */
public class AddLogRequest {
    private String message;

    public AddLogRequest() {
    }

    public AddLogRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

