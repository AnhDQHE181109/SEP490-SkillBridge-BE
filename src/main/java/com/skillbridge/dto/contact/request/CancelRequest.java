package com.skillbridge.dto.contact.request;

/**
 * Cancel Request DTO
 * Request DTO for cancelling a consultation
 */
public class CancelRequest {
    private String reason;

    public CancelRequest() {
    }

    public CancelRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

