package com.skillbridge.dto.contract.response;

/**
 * Project Close Request Response DTO
 * Response after creating, approving, rejecting, or resubmitting a project close request
 * Story-41: Project Close Request for SOW Contract
 */
public class ProjectCloseRequestResponse {
    private boolean success;
    private String message;
    private ProjectCloseRequestDetailDTO data;

    // Constructors
    public ProjectCloseRequestResponse() {
    }

    public ProjectCloseRequestResponse(boolean success, String message, ProjectCloseRequestDetailDTO data) {
        this.success = success;
        this.message = message;
        this.data = data;
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

    public ProjectCloseRequestDetailDTO getData() {
        return data;
    }

    public void setData(ProjectCloseRequestDetailDTO data) {
        this.data = data;
    }
}

