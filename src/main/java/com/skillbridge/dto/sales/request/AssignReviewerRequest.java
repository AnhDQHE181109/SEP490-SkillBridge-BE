package com.skillbridge.dto.sales.request;

/**
 * DTO for assigning reviewer to proposal
 */
public class AssignReviewerRequest {
    private Integer reviewerId;

    public AssignReviewerRequest() {
    }

    public AssignReviewerRequest(Integer reviewerId) {
        this.reviewerId = reviewerId;
    }

    public Integer getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Integer reviewerId) {
        this.reviewerId = reviewerId;
    }
}

