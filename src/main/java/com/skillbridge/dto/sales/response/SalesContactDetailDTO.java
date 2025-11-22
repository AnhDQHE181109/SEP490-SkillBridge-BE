package com.skillbridge.dto.sales.response;

import com.skillbridge.dto.contact.response.CommunicationLogDTO;
import java.util.List;

/**
 * Sales Contact Detail DTO
 * Extended DTO for sales contact detail with additional fields
 */
public class SalesContactDetailDTO {
    private String id; // Format: CC-YYYY-NN
    private Integer contactId; // Internal ID
    private String dateReceived; // Format: YYYY/MM/DD HH:MM JST
    private String clientName;
    private String email;
    private String phone;
    private String clientCompany;
    private String consultationRequest;
    private String requestType; // PROJECT, HIRING, CONSULTATION
    private String status; // NEW, INPROGRESS, CONVERTED_TO_OPPORTUNITY, CLOSED
    private String priority; // NORMAL, HIGH, URGENT
    private Integer assigneeUserId;
    private String assigneeName;
    private Boolean convertedToOpportunity;
    private String internalNotes;
    private String onlineMtgLink;
    private String onlineMtgDateTime; // Format: YYYY/MM/DD HH:MM
    private List<CommunicationLogDTO> communicationLogs;

    // Constructors
    public SalesContactDetailDTO() {
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getContactId() {
        return contactId;
    }

    public void setContactId(Integer contactId) {
        this.contactId = contactId;
    }

    public String getDateReceived() {
        return dateReceived;
    }

    public void setDateReceived(String dateReceived) {
        this.dateReceived = dateReceived;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getClientCompany() {
        return clientCompany;
    }

    public void setClientCompany(String clientCompany) {
        this.clientCompany = clientCompany;
    }

    public String getConsultationRequest() {
        return consultationRequest;
    }

    public void setConsultationRequest(String consultationRequest) {
        this.consultationRequest = consultationRequest;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public Boolean getConvertedToOpportunity() {
        return convertedToOpportunity;
    }

    public void setConvertedToOpportunity(Boolean convertedToOpportunity) {
        this.convertedToOpportunity = convertedToOpportunity;
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

    public List<CommunicationLogDTO> getCommunicationLogs() {
        return communicationLogs;
    }

    public void setCommunicationLogs(List<CommunicationLogDTO> communicationLogs) {
        this.communicationLogs = communicationLogs;
    }
}

