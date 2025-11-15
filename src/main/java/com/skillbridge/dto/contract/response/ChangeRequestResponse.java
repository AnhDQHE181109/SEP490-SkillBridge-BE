package com.skillbridge.dto.contract.response;

/**
 * Change Request Response DTO
 * Response after creating or saving a change request
 */
public class ChangeRequestResponse {
    private boolean success;
    private String message;
    private Integer changeRequestId;
    private String changeRequestDisplayId; // Format: CR-YYYY-NN

    // Constructors
    public ChangeRequestResponse() {
    }

    public ChangeRequestResponse(boolean success, String message, Integer changeRequestId, String changeRequestDisplayId) {
        this.success = success;
        this.message = message;
        this.changeRequestId = changeRequestId;
        this.changeRequestDisplayId = changeRequestDisplayId;
    }

    // Getters and Setters
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

    public Integer getChangeRequestId() {
        return changeRequestId;
    }

    public void setChangeRequestId(Integer changeRequestId) {
        this.changeRequestId = changeRequestId;
    }

    public String getChangeRequestDisplayId() {
        return changeRequestDisplayId;
    }

    public void setChangeRequestDisplayId(String changeRequestDisplayId) {
        this.changeRequestDisplayId = changeRequestDisplayId;
    }
}

