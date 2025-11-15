package com.skillbridge.dto.auth.response;

/**
 * Logout Response DTO
 * Response body for logout endpoint
 */
public class LogoutResponse {

    private String message;

    // Constructors
    public LogoutResponse() {
    }

    public LogoutResponse(String message) {
        this.message = message;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

