package com.skillbridge.dto.sales.response;

import java.time.LocalDateTime;

/**
 * DTO for Proposal Version in the versions table
 */
public class ProposalVersionDTO {
    private Integer id;
    private Integer version;
    private String name; // e.g., "v1", "v2", "v3"
    private String title;
    private Integer createdBy;
    private String createdByName; // e.g., "Sale-01"
    private LocalDateTime createdAt;
    private String status; // DRAFT, INTERNAL_REVIEW, REVIEWED, SENT_TO_CLIENT, CLIENT_REQUESTS_CHANGE, CLIENT_REJECTED, CLIENT_ACCEPTED
    private Integer reviewerId;
    private String reviewerName; // e.g., "Sales Manager 01"
    private String internalFeedback; // Review notes
    private String clientFeedback;
    private Boolean isCurrent;

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Integer reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getInternalFeedback() {
        return internalFeedback;
    }

    public void setInternalFeedback(String internalFeedback) {
        this.internalFeedback = internalFeedback;
    }

    public String getClientFeedback() {
        return clientFeedback;
    }

    public void setClientFeedback(String clientFeedback) {
        this.clientFeedback = clientFeedback;
    }

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }
}

