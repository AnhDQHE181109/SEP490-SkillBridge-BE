package com.skillbridge.dto.contact.response;

/**
 * Approve Response DTO
 * Response DTO for approving a proposal
 */
public class ApproveResponse {
    private boolean success;
    private String message;

    public ApproveResponse() {
    }

    public ApproveResponse(boolean success, String message) {
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

