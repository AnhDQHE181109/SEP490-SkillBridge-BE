package com.skillbridge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Contact Entity
 * Represents a contact request/submission from a guest/client
 */
@Entity
@Table(name = "contacts")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "client_user_id")
    private Integer clientUserId;

    @Column(name = "assignee_user_id")
    private Integer assigneeUserId;

    @Column(name = "reviewer_id")
    private Integer reviewerId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", length = 32)
    private String status = "New";

    @Column(name = "request_type", length = 32)
    private String requestType;

    @Column(name = "priority", length = 32)
    private String priority = "Medium";

    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;

    @Column(name = "online_mtg_link", length = 255)
    private String onlineMtgLink;

    @Column(name = "online_mtg_date")
    private LocalDateTime onlineMtgDate;

    @Column(name = "communication_progress", length = 32)
    private String communicationProgress = "AutoReply";

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Many-to-One relationship with User (client)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_user_id", insertable = false, updatable = false)
    private User clientUser;

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
    public Contact() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getClientUserId() {
        return clientUserId;
    }

    public void setClientUserId(Integer clientUserId) {
        this.clientUserId = clientUserId;
    }

    public Integer getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(Integer assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public Integer getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Integer reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getInternalNote() {
        return internalNote;
    }

    public void setInternalNote(String internalNote) {
        this.internalNote = internalNote;
    }

    public String getOnlineMtgLink() {
        return onlineMtgLink;
    }

    public void setOnlineMtgLink(String onlineMtgLink) {
        this.onlineMtgLink = onlineMtgLink;
    }

    public LocalDateTime getOnlineMtgDate() {
        return onlineMtgDate;
    }

    public void setOnlineMtgDate(LocalDateTime onlineMtgDate) {
        this.onlineMtgDate = onlineMtgDate;
    }

    public String getCommunicationProgress() {
        return communicationProgress;
    }

    public void setCommunicationProgress(String communicationProgress) {
        this.communicationProgress = communicationProgress;
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

    public User getClientUser() {
        return clientUser;
    }

    public void setClientUser(User clientUser) {
        this.clientUser = clientUser;
    }
}

