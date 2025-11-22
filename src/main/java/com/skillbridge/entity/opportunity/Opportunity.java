package com.skillbridge.entity.opportunity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Opportunity Entity
 * Represents a sales opportunity
 */
@Entity
@Table(name = "opportunities")
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "opportunity_id", unique = true, nullable = false, length = 50)
    private String opportunityId; // e.g., "OP-2025-01"

    @Column(name = "contact_id")
    private Integer contactId; // Foreign key to contacts table

    @Column(name = "est_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal estValue;

    @Column(name = "currency", length = 10)
    private String currency = "JPY";

    @Column(name = "probability")
    private Integer probability = 0; // Percentage (0-100)

    @Column(name = "client_email", nullable = false, length = 255)
    private String clientEmail;

    @Column(name = "client_name", nullable = false, length = 255)
    private String clientName;

    @Column(name = "client_company", length = 255)
    private String clientCompany;

    @Column(name = "status", length = 32)
    private String status = "NEW"; // NEW, IN_PROGRESS, PROPOSAL_DRAFTING, PROPOSAL_SENT, REVISION, WON, LOST

    @Column(name = "assignee_user_id")
    private Integer assigneeUserId; // Foreign key to users table

    @Column(name = "created_by", nullable = false)
    private Integer createdBy; // Foreign key to users table

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public String getClientCompany() {
        return clientCompany;
    }

    public void setClientCompany(String clientCompany) {
        this.clientCompany = clientCompany;
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

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

