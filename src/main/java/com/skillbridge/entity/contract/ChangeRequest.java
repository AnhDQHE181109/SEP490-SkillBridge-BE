package com.skillbridge.entity.contract;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Change Request Entity
 * Represents a change request for a contract (SOW)
 */
@Entity
@Table(name = "change_requests")
public class ChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "contract_id", nullable = true)
    private Integer contractId; // For MSA contracts

    @Column(name = "sow_contract_id", nullable = true)
    private Integer sowContractId; // For SOW contracts

    @Column(name = "contract_type", length = 10)
    private String contractType; // "MSA" or "SOW"

    @Column(name = "change_request_id", nullable = false, unique = true, length = 50)
    private String changeRequestId; // Format: CR-YYYY-NN

    @Column(name = "type", nullable = false, length = 50)
    private String type; // "Add Scope", "Extend", "Reduce"

    @Column(name = "title", length = 255)
    private String title; // CR Title

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Detailed description

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason; // Reason for change request

    @Column(name = "planned_end")
    private LocalDate plannedEnd; // For Fixed Price SOW

    @Column(name = "effective_from")
    private LocalDate effectiveFrom; // For Retainer SOW

    @Column(name = "effective_until")
    private LocalDate effectiveUntil; // For Retainer SOW

    @Column(name = "desired_start_date")
    private LocalDate desiredStartDate; // Desired start date for change

    @Column(name = "desired_end_date")
    private LocalDate desiredEndDate; // Desired end date for change

    @Column(name = "amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    @Column(name = "expected_extra_cost", precision = 16, scale = 2)
    private BigDecimal expectedExtraCost; // Expected extra cost from client

    @Column(name = "cost_estimated_by_landbridge", precision = 16, scale = 2)
    private BigDecimal costEstimatedByLandbridge; // Cost estimated by LandBridge

    @Column(name = "dev_hours")
    private Integer devHours; // For Fixed Price impact analysis

    @Column(name = "test_hours")
    private Integer testHours; // For Fixed Price impact analysis

    @Column(name = "new_end_date")
    private LocalDate newEndDate; // For Fixed Price impact analysis

    @Column(name = "delay_duration")
    private Integer delayDuration; // For Fixed Price impact analysis (in days)

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence; // JSON array of evidence links/files

    @Column(name = "status", nullable = false, length = 50)
    private String status = "Pending"; // "Draft", "Pending", "Processing", "Under Review", "Approved", "Request for Change", "Active", "Terminated", "Rejected"

    @Column(name = "created_by")
    private Integer createdBy; // User ID who created the change request

    @Column(name = "internal_reviewer_id")
    private Integer internalReviewerId; // User ID of the internal reviewer assigned to review the change request

    @Column(name = "created_at", nullable = false, updatable = false)
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

    // Constructors
    public ChangeRequest() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getContractId() {
        return contractId;
    }

    public void setContractId(Integer contractId) {
        this.contractId = contractId;
    }

    public Integer getSowContractId() {
        return sowContractId;
    }

    public void setSowContractId(Integer sowContractId) {
        this.sowContractId = sowContractId;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public String getChangeRequestId() {
        return changeRequestId;
    }

    public void setChangeRequestId(String changeRequestId) {
        this.changeRequestId = changeRequestId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getPlannedEnd() {
        return plannedEnd;
    }

    public void setPlannedEnd(LocalDate plannedEnd) {
        this.plannedEnd = plannedEnd;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveUntil() {
        return effectiveUntil;
    }

    public void setEffectiveUntil(LocalDate effectiveUntil) {
        this.effectiveUntil = effectiveUntil;
    }

    public LocalDate getDesiredStartDate() {
        return desiredStartDate;
    }

    public void setDesiredStartDate(LocalDate desiredStartDate) {
        this.desiredStartDate = desiredStartDate;
    }

    public LocalDate getDesiredEndDate() {
        return desiredEndDate;
    }

    public void setDesiredEndDate(LocalDate desiredEndDate) {
        this.desiredEndDate = desiredEndDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getExpectedExtraCost() {
        return expectedExtraCost;
    }

    public void setExpectedExtraCost(BigDecimal expectedExtraCost) {
        this.expectedExtraCost = expectedExtraCost;
    }

    public BigDecimal getCostEstimatedByLandbridge() {
        return costEstimatedByLandbridge;
    }

    public void setCostEstimatedByLandbridge(BigDecimal costEstimatedByLandbridge) {
        this.costEstimatedByLandbridge = costEstimatedByLandbridge;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getInternalReviewerId() {
        return internalReviewerId;
    }

    public void setInternalReviewerId(Integer internalReviewerId) {
        this.internalReviewerId = internalReviewerId;
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

    public Integer getDevHours() {
        return devHours;
    }

    public void setDevHours(Integer devHours) {
        this.devHours = devHours;
    }

    public Integer getTestHours() {
        return testHours;
    }

    public void setTestHours(Integer testHours) {
        this.testHours = testHours;
    }

    public LocalDate getNewEndDate() {
        return newEndDate;
    }

    public void setNewEndDate(LocalDate newEndDate) {
        this.newEndDate = newEndDate;
    }

    public Integer getDelayDuration() {
        return delayDuration;
    }

    public void setDelayDuration(Integer delayDuration) {
        this.delayDuration = delayDuration;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }
}

