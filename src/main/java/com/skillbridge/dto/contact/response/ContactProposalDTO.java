package com.skillbridge.dto.contact.response;

import java.util.List;

import com.skillbridge.dto.sales.response.SalesChangeRequestDetailDTO.AttachmentDTO;

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
    private List<AttachmentDTO> attachments; // List of attachments with s3Key and fileName

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

    public List<AttachmentDTO> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentDTO> attachments) {
        this.attachments = attachments;
    }
}

