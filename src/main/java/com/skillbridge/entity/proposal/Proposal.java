package com.skillbridge.entity.proposal;

import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contact.Contact;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Proposal Entity
 * Represents a proposal linked to a contact
 */
@Entity
@Table(name = "proposals")
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "is_current")
    private Boolean isCurrent = false;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "status", length = 50, nullable = false)
    private String status = "draft"; // "draft", "under review", "Sent to client", "Reject", "Approved"

    @Column(name = "reviewer_id")
    private Integer reviewerId;

    @Column(name = "contact_id")
    private Integer contactId; // Can be null if created from opportunity

    @Column(name = "opportunity_id")
    private Integer opportunityId; // Foreign key to opportunities table

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes; // Review notes from assigned reviewer

    @Column(name = "review_action", length = 32)
    private String reviewAction; // APPROVE, REQUEST_REVISION, REJECT

    @Column(name = "review_submitted_at")
    private LocalDateTime reviewSubmittedAt; // When review was submitted

    @Column(name = "link", length = 500)
    private String link; // Link to S3 document (first file)

    @Column(name = "attachments_manifest", columnDefinition = "TEXT")
    private String attachmentsManifest; // JSON string containing array of file links

    @Column(name = "client_feedback", columnDefinition = "TEXT")
    private String clientFeedback; // Feedback from client

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Many-to-One relationship with Contact (optional, for backward compatibility)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", insertable = false, updatable = false)
    private Contact contact;

    // Many-to-One relationship with Opportunity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", insertable = false, updatable = false)
    private com.skillbridge.entity.opportunity.Opportunity opportunity;

    // Many-to-One relationship with User (reviewer - sales manager)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", insertable = false, updatable = false)
    private User reviewer;

    // Many-to-One relationship with User (creator)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User creator;

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
    public Proposal() {
    }

    // Getters and Setters
    public Integer getID() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Integer reviewerId) {
        this.reviewerId = reviewerId;
    }

    public Integer getContactID() {
        return contactId;
    }

    public void setContactId(Integer contactId) {
        this.contactId = contactId;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
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

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public User getReviewer() {
        return reviewer;
    }

    public void setReviewer(User reviewer) {
        this.reviewer = reviewer;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public Integer getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(Integer opportunityId) {
        this.opportunityId = opportunityId;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public String getReviewAction() {
        return reviewAction;
    }

    public void setReviewAction(String reviewAction) {
        this.reviewAction = reviewAction;
    }

    public LocalDateTime getReviewSubmittedAt() {
        return reviewSubmittedAt;
    }

    public void setReviewSubmittedAt(LocalDateTime reviewSubmittedAt) {
        this.reviewSubmittedAt = reviewSubmittedAt;
    }

    public String getAttachmentsManifest() {
        return attachmentsManifest;
    }

    public void setAttachmentsManifest(String attachmentsManifest) {
        this.attachmentsManifest = attachmentsManifest;
    }

    public com.skillbridge.entity.opportunity.Opportunity getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(com.skillbridge.entity.opportunity.Opportunity opportunity) {
        this.opportunity = opportunity;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    public String getClientFeedback() {
        return clientFeedback;
    }

    public void setClientFeedback(String clientFeedback) {
        this.clientFeedback = clientFeedback;
    }
}

