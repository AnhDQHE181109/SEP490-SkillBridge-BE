package com.skillbridge.dto.sales.response;

import java.math.BigDecimal;

/**
 * DTO for opportunity list item
 */
public class OpportunityListItemDTO {
    private Integer id;
    private String opportunityId; // e.g., "OP-2025-01"
    private BigDecimal estValue;
    private String currency;
    private Integer probability;
    private String clientEmail;
    private String clientName;
    private String status;
    private Integer assigneeUserId;
    private String assigneeName;
    private Integer createdBy;
    private String createdByName;

    public OpportunityListItemDTO() {
    }

    public OpportunityListItemDTO(Integer id, String opportunityId, BigDecimal estValue, String currency,
                                  Integer probability, String clientEmail, String clientName, String status,
                                  Integer assigneeUserId, String assigneeName, Integer createdBy, String createdByName) {
        this.id = id;
        this.opportunityId = opportunityId;
        this.estValue = estValue;
        this.currency = currency;
        this.probability = probability;
        this.clientEmail = clientEmail;
        this.clientName = clientName;
        this.status = status;
        this.assigneeUserId = assigneeUserId;
        this.assigneeName = assigneeName;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
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

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
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
}

