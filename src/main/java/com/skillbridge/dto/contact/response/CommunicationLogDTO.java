package com.skillbridge.dto.contact.response;

/**
 * Communication Log DTO
 * Data transfer object for communication log entries
 */
public class CommunicationLogDTO {
    private Integer id;
    private String message;
    private String createdAt; // Format: YYYY/MM/DD HH:MM
    private Integer createdBy;
    private String createdByName;

    // Constructors
    public CommunicationLogDTO() {
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
}

