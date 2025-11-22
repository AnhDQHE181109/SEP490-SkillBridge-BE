package com.skillbridge.entity.proposal;

import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.opportunity.Opportunity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Proposal History Entity
 * Tracks proposal activities and changes
 */
@Entity
@Table(name = "proposal_history")
public class ProposalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "opportunity_id", nullable = false)
    private Integer opportunityId;

    @Column(name = "proposal_id")
    private Integer proposalId;

    @Column(name = "activity_type", length = 50, nullable = false)
    private String activityType; // CREATED, UPLOADED, SENT, REVIEWED, APPROVED, REJECTED, CONVERTED

    @Column(name = "activity_description", columnDefinition = "TEXT")
    private String activityDescription;

    @Column(name = "file_link", length = 500)
    private String fileLink;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Many-to-One relationship with Opportunity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", insertable = false, updatable = false)
    private Opportunity opportunity;

    // Many-to-One relationship with Proposal
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", insertable = false, updatable = false)
    private Proposal proposal;

    // Many-to-One relationship with User (creator)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User creator;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public ProposalHistory() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(Integer opportunityId) {
        this.opportunityId = opportunityId;
    }

    public Integer getProposalId() {
        return proposalId;
    }

    public void setProposalId(Integer proposalId) {
        this.proposalId = proposalId;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getActivityDescription() {
        return activityDescription;
    }

    public void setActivityDescription(String activityDescription) {
        this.activityDescription = activityDescription;
    }

    public String getFileLink() {
        return fileLink;
    }

    public void setFileLink(String fileLink) {
        this.fileLink = fileLink;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
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

    public Opportunity getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(Opportunity opportunity) {
        this.opportunity = opportunity;
    }

    public Proposal getProposal() {
        return proposal;
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }
}

