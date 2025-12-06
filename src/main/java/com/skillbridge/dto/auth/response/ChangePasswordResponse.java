package com.skillbridge.dto.auth.response;

/**
 * Change Password Response DTO
 * Response DTO for change password operation
 */
public class ChangePasswordResponse {
    
    private boolean success;
    private String message;
    
    // Constructors
    public ChangePasswordResponse() {
    }
    
    public ChangePasswordResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
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
}

