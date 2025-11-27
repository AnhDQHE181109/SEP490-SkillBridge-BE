package com.skillbridge.entity.contract;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CR Billing Event Entity
 * Represents a billing change event from a Change Request
 * Tracks delta amounts (positive or negative) for billing months
 */
@Entity
@Table(name = "cr_billing_events")
public class CRBillingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "change_request_id", nullable = false)
    private Integer changeRequestId;

    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth; // YYYY-MM-01 format

    @Column(name = "delta_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal deltaAmount; // Positive or negative

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private BillingEventType type; // RETAINER_ADJUST, SCOPE_ADJUSTMENT, CORRECTION

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum BillingEventType {
        RETAINER_ADJUST, SCOPE_ADJUSTMENT, CORRECTION
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public CRBillingEvent() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getChangeRequestId() {
        return changeRequestId;
    }

    public void setChangeRequestId(Integer changeRequestId) {
        this.changeRequestId = changeRequestId;
    }

    public LocalDate getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(LocalDate billingMonth) {
        this.billingMonth = billingMonth;
    }

    public BigDecimal getDeltaAmount() {
        return deltaAmount;
    }

    public void setDeltaAmount(BigDecimal deltaAmount) {
        this.deltaAmount = deltaAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BillingEventType getType() {
        return type;
    }

    public void setType(BillingEventType type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

