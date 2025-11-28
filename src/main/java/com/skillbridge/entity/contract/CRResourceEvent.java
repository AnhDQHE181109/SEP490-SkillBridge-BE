package com.skillbridge.entity.contract;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CR Resource Event Entity
 * Represents a resource change event from a Change Request
 * Tracks ADD, REMOVE, or MODIFY actions for engineers
 */
@Entity
@Table(name = "cr_resource_events")
public class CRResourceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "change_request_id", nullable = false)
    private Integer changeRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private ResourceAction action; // ADD, REMOVE, MODIFY

    @Column(name = "engineer_id")
    private Integer engineerId; // For MODIFY/REMOVE: reference to base engineer

    @Column(name = "role", length = 100)
    private String role;

    @Column(name = "level", length = 50)
    private String level;

    @Column(name = "rating_old", precision = 5, scale = 2)
    private BigDecimal ratingOld;

    @Column(name = "rating_new", precision = 5, scale = 2)
    private BigDecimal ratingNew;

    @Column(name = "unit_rate_old", precision = 16, scale = 2)
    private BigDecimal unitRateOld;

    @Column(name = "unit_rate_new", precision = 16, scale = 2)
    private BigDecimal unitRateNew;

    @Column(name = "start_date_old")
    private LocalDate startDateOld;

    @Column(name = "start_date_new")
    private LocalDate startDateNew;

    @Column(name = "end_date_old")
    private LocalDate endDateOld;

    @Column(name = "end_date_new")
    private LocalDate endDateNew;

    @Column(name = "effective_start", nullable = false)
    private LocalDate effectiveStart;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum ResourceAction {
        ADD, REMOVE, MODIFY
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public CRResourceEvent() {
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

    public ResourceAction getAction() {
        return action;
    }

    public void setAction(ResourceAction action) {
        this.action = action;
    }

    public Integer getEngineerId() {
        return engineerId;
    }

    public void setEngineerId(Integer engineerId) {
        this.engineerId = engineerId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public BigDecimal getRatingOld() {
        return ratingOld;
    }

    public void setRatingOld(BigDecimal ratingOld) {
        this.ratingOld = ratingOld;
    }

    public BigDecimal getRatingNew() {
        return ratingNew;
    }

    public void setRatingNew(BigDecimal ratingNew) {
        this.ratingNew = ratingNew;
    }

    public BigDecimal getUnitRateOld() {
        return unitRateOld;
    }

    public void setUnitRateOld(BigDecimal unitRateOld) {
        this.unitRateOld = unitRateOld;
    }

    public BigDecimal getUnitRateNew() {
        return unitRateNew;
    }

    public void setUnitRateNew(BigDecimal unitRateNew) {
        this.unitRateNew = unitRateNew;
    }

    public LocalDate getStartDateOld() {
        return startDateOld;
    }

    public void setStartDateOld(LocalDate startDateOld) {
        this.startDateOld = startDateOld;
    }

    public LocalDate getStartDateNew() {
        return startDateNew;
    }

    public void setStartDateNew(LocalDate startDateNew) {
        this.startDateNew = startDateNew;
    }

    public LocalDate getEndDateOld() {
        return endDateOld;
    }

    public void setEndDateOld(LocalDate endDateOld) {
        this.endDateOld = endDateOld;
    }

    public LocalDate getEndDateNew() {
        return endDateNew;
    }

    public void setEndDateNew(LocalDate endDateNew) {
        this.endDateNew = endDateNew;
    }

    public LocalDate getEffectiveStart() {
        return effectiveStart;
    }

    public void setEffectiveStart(LocalDate effectiveStart) {
        this.effectiveStart = effectiveStart;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

