package com.skillbridge.dto.contact.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contact List Item DTO
 * Data transfer object for contact list items
 */
public class ContactListItemDTO {

    private Integer no;
    
    @JsonProperty("internalId")
    private Integer internalId; // Primary key (database ID) - used for navigation
    
    private String id; // Display ID format: CT-yyyy-mm-dd-customer_id-contact_id (for display only)
    private String title;
    private String description;
    private String createdOn; // Format: YYYY/MM/DD
    private String status;

    // Constructors
    public ContactListItemDTO() {
    }

    public ContactListItemDTO(Integer no, Integer internalId, String id, String title, String description, String createdOn, String status) {
        this.no = no;
        this.internalId = internalId;
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdOn = createdOn;
        this.status = status;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}

