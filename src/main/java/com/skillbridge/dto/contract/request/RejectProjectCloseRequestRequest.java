package com.skillbridge.dto.contract.request;

/**
 * Reject Project Close Request Request DTO
 * Data transfer object for rejecting a project close request
 * Story-41: Project Close Request for SOW Contract
 */
public class RejectProjectCloseRequestRequest {
    private String reason; // Required (not empty, max 2000 characters)

    // Constructors
    public RejectProjectCloseRequestRequest() {
    }

    // Getters and Setters
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

