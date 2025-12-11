package com.skillbridge.dto.contact.request;

import jakarta.validation.constraints.Size;

/**
 * Cancel Request DTO
 * Request DTO for cancelling a consultation
 */
public class CancelRequest {
    @Size(max = 500, message = "Reason must not exceed 500 characters")
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

