package com.skillbridge.dto.sales.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Sales Contact List Item DTO
 * Data transfer object for sales contact list items
 */
public class SalesContactListItemDTO {

    private Integer no;

    @JsonProperty("contactId")
    private String contactId; // Format: CT-YYYY-NN or MSA-YYYY-NN

    @JsonProperty("clientName")
    private String clientName;

    @JsonProperty("clientEmail")
    private String clientEmail;

    @JsonProperty("company")
    private String company;

    @JsonProperty("title")
    private String title;

    @JsonProperty("status")
    private String status; // NEW, INPROGRESS, COMPLETED, CLOSED

    @JsonProperty("assigneeUserId")
    private Integer assigneeUserId;

    @JsonProperty("assigneeName")
    private String assigneeName; // Sales Man's full name

    @JsonProperty("internalId")
    private Integer internalId; // Database ID for navigation

    // Constructors
    public SalesContactListItemDTO() {
    }

    // Getters and Setters
    public Integer getNo() {
        return no;
    }

    public void setNo(Integer no) {
        this.no = no;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
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

    public Integer getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(Integer assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public Integer getInternalId() {
        return internalId;
    }

    public void setInternalId(Integer internalId) {
        this.internalId = internalId;
    }
}

