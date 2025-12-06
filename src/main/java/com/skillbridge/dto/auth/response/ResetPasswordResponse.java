package com.skillbridge.dto.auth.response;

/**
 * Reset Password Response DTO
 * Response DTO for reset password operation
 */
public class ResetPasswordResponse {
    
    private boolean success;
    private String message;
    
    // Constructors
    public ResetPasswordResponse() {
    }
    
    public ResetPasswordResponse(boolean success, String message) {
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

