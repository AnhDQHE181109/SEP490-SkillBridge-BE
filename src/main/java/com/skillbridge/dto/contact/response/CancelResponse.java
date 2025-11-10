package com.skillbridge.dto.contact.response;

/**
 * Cancel Response DTO
 * Response DTO for cancelling a consultation
 */
public class CancelResponse {
    private boolean success;
    private String message;

    public CancelResponse() {
    }

    public CancelResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

