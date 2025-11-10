package com.skillbridge.dto.contact.request;

/**
 * Comment Request DTO
 * Request DTO for adding a proposal comment
 */
public class CommentRequest {
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

