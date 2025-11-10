package com.skillbridge.dto.contact.response;

/**
 * Proposal Comment DTO
 * Data transfer object for proposal comment information
 */
public class ProposalCommentDTO {
    private String message;
    private String createdAt; // Format: YYYY/MM/DD HH:MM

    // Constructors
    public ProposalCommentDTO() {
    }

    public ProposalCommentDTO(String message, String createdAt) {
        this.message = message;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

