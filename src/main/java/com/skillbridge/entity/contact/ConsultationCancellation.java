package com.skillbridge.entity.contact;

import com.skillbridge.entity.auth.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Consultation Cancellation Entity
 * Represents a cancellation of a consultation with a reason
 */
@Entity
@Table(name = "consultation_cancellations")
public class ConsultationCancellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "contact_id", nullable = false)
    private Integer contactId;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "cancelled_by", nullable = false)
    private Integer cancelledBy;

    @Column(name = "cancelled_at", updatable = false)
    private LocalDateTime cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by", insertable = false, updatable = false)
    private User cancelledByUser;

    @PrePersist
    protected void onCreate() {
        cancelledAt = LocalDateTime.now();
    }

    // Constructors
    public ConsultationCancellation() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getContactId() {
        return contactId;
    }

    public void setContactId(Integer contactId) {
        this.contactId = contactId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(Integer cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public User getCancelledByUser() {
        return cancelledByUser;
    }

    public void setCancelledByUser(User cancelledByUser) {
        this.cancelledByUser = cancelledByUser;
    }
}

