package com.skillbridge.dto.sales.response;

/**
 * Change Request Response DTO
 * Response after creating a change request
 */
public class ChangeRequestResponseDTO {
    private Integer id;
    private String changeRequestId; // Format: CR-YYYY-NN
    private Boolean success;
    private String message;

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getChangeRequestId() {
        return changeRequestId;
    }

    public void setChangeRequestId(String changeRequestId) {
        this.changeRequestId = changeRequestId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

