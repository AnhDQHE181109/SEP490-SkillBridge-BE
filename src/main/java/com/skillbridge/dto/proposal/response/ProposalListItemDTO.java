package com.skillbridge.dto.proposal.response;

/**
 * Proposal List Item DTO
 * Represents a proposal in the list view
 */
public class ProposalListItemDTO {
    private Integer no;
    private Integer internalId; // Primary key for navigation
    private String id; // Format: P-YYYY-NN
    private String title;
    private String contactId; // Format: CT-YYYY-NN
    private Integer contactInternalId; // Contact primary key for navigation
    private String contactDescription;
    private String createdOn; // Format: YYYY/MM/DD
    private String status; // "Under review", "Request for change", "Approved"
    private String lastUpdated; // Format: YYYY/MM/DD

    // Constructors
    public ProposalListItemDTO() {
    }

    // Getters and Setters
    public Integer getNo() {
        return no;
    }

    public void setNo(Integer no) {
        this.no = no;
    }

    public Integer getInternalId() {
        return internalId;
    }

    public void setInternalId(Integer internalId) {
        this.internalId = internalId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public Integer getContactInternalId() {
        return contactInternalId;
    }

    public void setContactInternalId(Integer contactInternalId) {
        this.contactInternalId = contactInternalId;
    }

    public String getContactDescription() {
        return contactDescription;
    }

    public void setContactDescription(String contactDescription) {
        this.contactDescription = contactDescription;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}

