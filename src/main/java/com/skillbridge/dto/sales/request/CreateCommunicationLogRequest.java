package com.skillbridge.dto.sales.request;

/**
 * Create Communication Log Request DTO
 */
public class CreateCommunicationLogRequest {
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

