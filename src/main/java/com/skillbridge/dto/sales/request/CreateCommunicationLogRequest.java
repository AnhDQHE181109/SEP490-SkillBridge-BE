package com.skillbridge.dto.sales.request;

import jakarta.validation.constraints.Size;

/**
 * Create Communication Log Request DTO
 */
public class CreateCommunicationLogRequest {
    @Size(max = 500, message = "Communication log message must not exceed 500 characters")
    private String message;

    // Constructors
    public CreateCommunicationLogRequest() {
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

