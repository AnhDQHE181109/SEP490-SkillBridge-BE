package com.skillbridge.dto.dashboard.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Activity DTO
 * Represents a recent activity in the dashboard timeline
 */
public class ActivityDTO {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("date")
    private String date; // Format: YYYY/MM/DD

    @JsonProperty("description")
    private String description;

    @JsonProperty("type")
    private String type; // PROPOSAL, CONTRACT, CHANGE_REQUEST, CONTACT

    @JsonProperty("entityId")
    private Integer entityId;

    @JsonProperty("entityType")
    private String entityType; // proposal, contract, changeRequest, contact

    // Constructors
    public ActivityDTO() {
    }

    public ActivityDTO(Integer id, String date, String description, String type, Integer entityId, String entityType) {
        this.id = id;
        this.date = date;
        this.description = description;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

