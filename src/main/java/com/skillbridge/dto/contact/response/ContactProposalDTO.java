package com.skillbridge.dto.contact.response;

/**
 * Contact Proposal DTO
 * Data transfer object for proposal information in contact detail
 */
public class ContactProposalDTO {
    private Integer id;
    private Integer version;
    private String title;
    private String status; // Backend status: sent_to_client, revision_requested, approved, etc.
    private String proposalLink;
    private String proposalApprovedAt; // Format: YYYY/MM/DD HH:MM
    private String createdAt; // Format: YYYY/MM/DD HH:MM
    private Boolean isCurrent;
    private String clientFeedback; // Client feedback/comment for this proposal

    // Constructors
    public ContactProposalDTO() {
    }

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProposalLink() {
        return proposalLink;
    }

    public void setProposalLink(String proposalLink) {
        this.proposalLink = proposalLink;
    }

    public String getProposalApprovedAt() {
        return proposalApprovedAt;
    }

    public void setProposalApprovedAt(String proposalApprovedAt) {
        this.proposalApprovedAt = proposalApprovedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    public String getClientFeedback() {
        return clientFeedback;
    }

    public void setClientFeedback(String clientFeedback) {
        this.clientFeedback = clientFeedback;
    }
}

