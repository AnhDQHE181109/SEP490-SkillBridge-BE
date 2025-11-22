package com.skillbridge.dto.sales.request;

import java.math.BigDecimal;

/**
 * DTO for updating opportunity
 */
public class UpdateOpportunityRequest {
    private String clientName;
    private String clientCompany;
    private String clientEmail;
    private Integer assigneeUserId;
    private Integer probability;
    private BigDecimal estValue;
    private String currency;

    public UpdateOpportunityRequest() {
    }

    // Getters and Setters
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
}

