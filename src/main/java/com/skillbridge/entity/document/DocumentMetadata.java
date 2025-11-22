package com.skillbridge.entity.document;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Document Metadata Entity
 * Stores metadata and permissions for S3 documents
 */
@Entity
@Table(name = "document_metadata")
public class DocumentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "s3_key", nullable = false, unique = true, length = 500)
    private String s3Key;

    @Column(name = "owner_id", nullable = false)
    private Integer ownerId;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType; // proposal, contract, invoice, etc.

    @Column(name = "entity_id")
    private Integer entityId; // ID of related entity (proposal_id, contract_id, etc.)

    @Column(name = "entity_type", length = 50)
    private String entityType; // proposal, contract, etc.

    @Column(name = "allowed_roles", columnDefinition = "TEXT")
    private String allowedRoles; // JSON array of allowed roles

    @Column(name = "allowed_users", columnDefinition = "TEXT")
    private String allowedUsers; // JSON array of allowed user IDs

    @Column(name = "created_at", updatable = false)
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

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public Integer getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(String allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public String getAllowedUsers() {
        return allowedUsers;
    }

    public void setAllowedUsers(String allowedUsers) {
        this.allowedUsers = allowedUsers;
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

