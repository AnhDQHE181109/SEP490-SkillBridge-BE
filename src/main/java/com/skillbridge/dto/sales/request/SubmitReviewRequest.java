package com.skillbridge.dto.sales.request;

/**
 * Request DTO for submitting contract review
 */
public class SubmitReviewRequest {
    private String reviewNotes;
    private String action; // APPROVE, REQUEST_REVISION, REJECT

    // Getters and Setters
    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
