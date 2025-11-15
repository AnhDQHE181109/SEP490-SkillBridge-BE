package com.skillbridge.dto.contract.response;

/**
 * Contract History Item DTO
 * Data transfer object for contract history entry
 */
public class ContractHistoryItemDTO {
    private Integer id;
    private String date; // Format: YYYY/MM/DD
    private String description;
    private String documentLink; // URL to document
    private String documentName; // File name
    private String createdBy; // User who created the history entry

    // Constructors
    public ContractHistoryItemDTO() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDocumentLink() {
        return documentLink;
    }

    public void setDocumentLink(String documentLink) {
        this.documentLink = documentLink;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}

