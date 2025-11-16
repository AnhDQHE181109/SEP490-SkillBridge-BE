package com.skillbridge.entity.contract;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Contract Internal Review Entity
 * Stores internal review information for contracts (not visible to clients)
 */
@Entity
@Table(name = "contract_internal_review")
public class ContractInternalReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "contract_id", nullable = true)
    private Integer contractId; // For MSA contracts (nullable for SOW contracts)

    @Column(name = "sow_contract_id", nullable = true)
    private Integer sowContractId; // For SOW contracts (nullable)

    @Column(name = "contract_type", length = 10, nullable = false)
    private String contractType; // "MSA" or "SOW"

    @Column(name = "reviewer_id", nullable = false)
    private Integer reviewerId; // User ID of the reviewer

    @Column(name = "review_action", length = 32, nullable = false)
    private String reviewAction; // APPROVE, REQUEST_REVISION, REJECT

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes; // Review notes from reviewer

    @Column(name = "reviewed_at", nullable = false, updatable = false)
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (reviewedAt == null) {
            reviewedAt = LocalDateTime.now();
        }
    }

    // Constructors
    public ContractInternalReview() {
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

    public Integer getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Integer reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewAction() {
        return reviewAction;
    }

    public void setReviewAction(String reviewAction) {
        this.reviewAction = reviewAction;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

