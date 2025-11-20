package com.skillbridge.entity.contract;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Change Request History Entity
 * Represents a history entry for a change request
 */
@Entity
@Table(name = "change_request_history")
public class ChangeRequestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "change_request_id", nullable = false)
    private Integer changeRequestId;

    @Column(name = "action", nullable = false, length = 100)
    private String action; // "Created", "Submitted", "Approved", "Request for Change", "Terminated"

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "user_name", nullable = false, length = 255)
    private String userName;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // Constructors
    public ChangeRequestHistory() {
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

