package com.skillbridge.dto.contact.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Comment Request DTO
 * Request DTO for adding a proposal comment
 */
public class CommentRequest {
    @NotBlank(message = "Message is required")
    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;

    public CommentRequest() {
    }

    public CommentRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

