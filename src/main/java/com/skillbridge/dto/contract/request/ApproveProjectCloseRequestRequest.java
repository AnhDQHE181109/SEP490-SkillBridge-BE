package com.skillbridge.dto.contract.request;

/**
 * Approve Project Close Request Request DTO
 * Data transfer object for approving a project close request
 * Story-41: Project Close Request for SOW Contract
 */
public class ApproveProjectCloseRequestRequest {
    private Boolean confirm; // Optional confirmation flag

    // Constructors
    public ApproveProjectCloseRequestRequest() {
    }

    // Getters and Setters
    public Boolean getConfirm() {
        return confirm;
    }

    public void setConfirm(Boolean confirm) {
        this.confirm = confirm;
    }
}

