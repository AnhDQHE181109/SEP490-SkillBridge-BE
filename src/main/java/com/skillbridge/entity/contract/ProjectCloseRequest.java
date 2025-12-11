package com.skillbridge.entity.contract;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Project Close Request Entity
 * Represents a request from Sales Representative to close a SOW contract
 * Story-41: Project Close Request for SOW Contract
 */
@Entity
@Table(name = "project_close_requests")
public class ProjectCloseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "sow_id", nullable = false)
    private Integer sowId;

    @Column(name = "requested_by_user_id", nullable = false)
    private Integer requestedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(50)")
    private ProjectCloseRequestStatus status = ProjectCloseRequestStatus.Pending;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message; // Thank you message / notes from SalesRep

    @Column(name = "links", columnDefinition = "TEXT")
    private String links; // URLs to documents, feedback forms, etc. (one per line or JSON array)

    @Column(name = "client_reject_reason", columnDefinition = "TEXT")
    private String clientRejectReason; // Reason provided by Client when rejecting

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
    public ProjectCloseRequest() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSowId() {
        return sowId;
    }

    public void setSowId(Integer sowId) {
        this.sowId = sowId;
    }

    public Integer getRequestedByUserId() {
        return requestedByUserId;
    }

    public void setRequestedByUserId(Integer requestedByUserId) {
        this.requestedByUserId = requestedByUserId;
    }

    public ProjectCloseRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectCloseRequestStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLinks() {
        return links;
    }

    public void setLinks(String links) {
        this.links = links;
    }

    public String getClientRejectReason() {
        return clientRejectReason;
    }

    public void setClientRejectReason(String clientRejectReason) {
        this.clientRejectReason = clientRejectReason;
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

    // Enums
    public enum ProjectCloseRequestStatus {
        Pending, ClientApproved, Rejected
    }
}

