package com.skillbridge.dto.dashboard.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Alert DTO
 * Represents an alert/notification in the dashboard
 */
public class AlertDTO {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("message")
    private String message;

    @JsonProperty("priority")
    private String priority; // HIGH, MEDIUM, LOW

    @JsonProperty("type")
    private String type; // CHANGE_REQUEST_DECISION, CONTRACT_SIGNATURE, PROPOSAL_REVIEW, etc.

    @JsonProperty("entityId")
    private Integer entityId;

    @JsonProperty("entityType")
    private String entityType; // changeRequest, contract, proposal, contact

    // Constructors
    public AlertDTO() {
    }

    public AlertDTO(Integer id, String message, String priority, String type, Integer entityId, String entityType) {
        this.id = id;
        this.message = message;
        this.priority = priority;
        this.type = type;
        this.entityId = entityId;
        this.entityType = entityType;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
}

