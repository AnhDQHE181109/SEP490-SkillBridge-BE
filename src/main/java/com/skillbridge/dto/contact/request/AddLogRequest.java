package com.skillbridge.dto.contact.request;

import jakarta.validation.constraints.Size;

/**
 * Add Log Request DTO
 * Request DTO for adding a communication log
 */
public class AddLogRequest {
    @Size(max = 500, message = "Communication log message must not exceed 500 characters")
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

