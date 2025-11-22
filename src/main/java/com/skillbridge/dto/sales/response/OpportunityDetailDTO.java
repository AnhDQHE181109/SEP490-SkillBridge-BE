package com.skillbridge.dto.sales.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for opportunity detail response
 */
public class OpportunityDetailDTO {
    private Integer id;
    private String opportunityId; // e.g., "OP-2025-01"
    private Integer contactId;
    private String clientName;
    private String clientCompany;
    private String clientEmail;
    private Integer assigneeUserId;
    private String assigneeName;
    private Integer probability;
    private BigDecimal estValue;
    private String currency;
    private String status;
    private String stage; // "New", "Proposal", "Won", "Lost"
    private Integer createdBy;
    private String createdByName;
    private ProposalDTO proposal; // Optional, current proposal if exists
    private List<ProposalVersionDTO> proposalVersions; // All proposal versions
    private List<HistoryEntryDTO> history; // History log
    private Boolean canConvertToContract; // True if client approved a proposal

    public OpportunityDetailDTO() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(String opportunityId) {
        this.opportunityId = opportunityId;
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

    public String getClientCompany() {
        return clientCompany;
    }

    public void setClientCompany(String clientCompany) {
        this.clientCompany = clientCompany;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
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

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public BigDecimal getEstValue() {
        return estValue;
    }

    public void setEstValue(BigDecimal estValue) {
        this.estValue = estValue;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
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

    public ProposalDTO getProposal() {
        return proposal;
    }

    public void setProposal(ProposalDTO proposal) {
        this.proposal = proposal;
    }

    public List<ProposalVersionDTO> getProposalVersions() {
        return proposalVersions;
    }

    public void setProposalVersions(List<ProposalVersionDTO> proposalVersions) {
        this.proposalVersions = proposalVersions;
    }

    public List<HistoryEntryDTO> getHistory() {
        return history;
    }

    public void setHistory(List<HistoryEntryDTO> history) {
        this.history = history;
    }

    public Boolean getCanConvertToContract() {
        return canConvertToContract;
    }

    public void setCanConvertToContract(Boolean canConvertToContract) {
        this.canConvertToContract = canConvertToContract;
    }
}

