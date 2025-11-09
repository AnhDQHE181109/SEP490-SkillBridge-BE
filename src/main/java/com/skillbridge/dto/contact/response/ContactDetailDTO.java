package com.skillbridge.dto.contact.response;

import java.util.List;

/**
 * Contact Detail DTO
 * Data transfer object for contact detail information
 */
public class ContactDetailDTO {
    private String id; // Format: CC-YYYY-NN (same as ContactListItemDTO format)
    private Integer contactId; // Internal ID
    private String clientName;
    private String phone;
    private String email;
    private String clientCompany;
    private String dateReceived; // Format: YYYY/MM/DD HH:MM JST
    private String consultationRequest;
    private String onlineMtgDate; // Format: YYYY/MM/DD HH:MM
    private String onlineMtgLink;
    private String status;
    private String proposalLink;
    private String proposalStatus;
    private String proposalApprovedAt; // Format: YYYY/MM/DD HH:MM
    private ProposalCommentDTO proposalComment; // Latest comment if exists
    private List<CommunicationLogDTO> communicationLogs;

    // Constructors
    public ContactDetailDTO() {
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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getClientCompany() {
        return clientCompany;
    }

    public void setClientCompany(String clientCompany) {
        this.clientCompany = clientCompany;
    }

    public String getDateReceived() {
        return dateReceived;
    }

    public void setDateReceived(String dateReceived) {
        this.dateReceived = dateReceived;
    }

    public String getConsultationRequest() {
        return consultationRequest;
    }

    public void setConsultationRequest(String consultationRequest) {
        this.consultationRequest = consultationRequest;
    }

    public String getOnlineMtgDate() {
        return onlineMtgDate;
    }

    public void setOnlineMtgDate(String onlineMtgDate) {
        this.onlineMtgDate = onlineMtgDate;
    }

    public String getOnlineMtgLink() {
        return onlineMtgLink;
    }

    public void setOnlineMtgLink(String onlineMtgLink) {
        this.onlineMtgLink = onlineMtgLink;
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

    public String getProposalStatus() {
        return proposalStatus;
    }

    public void setProposalStatus(String proposalStatus) {
        this.proposalStatus = proposalStatus;
    }

    public String getProposalApprovedAt() {
        return proposalApprovedAt;
    }

    public void setProposalApprovedAt(String proposalApprovedAt) {
        this.proposalApprovedAt = proposalApprovedAt;
    }

    public ProposalCommentDTO getProposalComment() {
        return proposalComment;
    }

    public void setProposalComment(ProposalCommentDTO proposalComment) {
        this.proposalComment = proposalComment;
    }

    public List<CommunicationLogDTO> getCommunicationLogs() {
        return communicationLogs;
    }

    public void setCommunicationLogs(List<CommunicationLogDTO> communicationLogs) {
        this.communicationLogs = communicationLogs;
    }
}

