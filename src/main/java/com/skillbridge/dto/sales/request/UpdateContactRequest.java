package com.skillbridge.dto.sales.request;

/**
 * Update Contact Request DTO
 * Used for updating contact fields with role-based permissions
 */
public class UpdateContactRequest {
    private String requestType; // PROJECT, HIRING, CONSULTATION - Only Sales Manager can update
    private String priority; // NORMAL, HIGH, URGENT - Only Sales Manager can update
    private Integer assigneeUserId; // Only Sales Manager can update
    private String status; // NEW, INPROGRESS, CONVERTED_TO_OPPORTUNITY, CLOSED - Only assigned Sales Man can update
    private String internalNotes; // Only assigned Sales Man can update
    private String onlineMtgLink; // Only assigned Sales Man can update
    private String onlineMtgDateTime; // Format: YYYY/MM/DD HH:MM - Only assigned Sales Man can update

    // Constructors
    public UpdateContactRequest() {
    }

    // Getters and Setters
    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Integer getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(Integer assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInternalNotes() {
        return internalNotes;
    }

    public void setInternalNotes(String internalNotes) {
        this.internalNotes = internalNotes;
    }

    public String getOnlineMtgLink() {
        return onlineMtgLink;
    }

    public void setOnlineMtgLink(String onlineMtgLink) {
        this.onlineMtgLink = onlineMtgLink;
    }

    public String getOnlineMtgDateTime() {
        return onlineMtgDateTime;
    }

    public void setOnlineMtgDateTime(String onlineMtgDateTime) {
        this.onlineMtgDateTime = onlineMtgDateTime;
    }
}

