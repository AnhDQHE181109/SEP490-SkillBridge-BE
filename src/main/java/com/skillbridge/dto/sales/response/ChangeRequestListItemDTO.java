package com.skillbridge.dto.sales.response;

/**
 * Change Request List Item DTO
 * Data transfer object for change request list items in Sales Portal
 */
public class ChangeRequestListItemDTO {
    private Integer id;
    private String changeRequestId; // Format: CR-YYYY-NN
    private String type; // "Extend Schedule", "Rate Change", "Add Scope", etc.
    private String summary;
    private String effectiveFrom; // Format: YYYY-MM-DD or null
    private String effectiveUntil; // Format: YYYY-MM-DD or null
    private Double expectedExtraCost; // Expected extra cost from client
    private Double costEstimatedByLandbridge; // Cost estimated by LandBridge
    private String status; // "Draft", "Pending", "Processing", etc.

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getChangeRequestId() {
        return changeRequestId;
    }

    public void setChangeRequestId(String changeRequestId) {
        this.changeRequestId = changeRequestId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(String effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public String getEffectiveUntil() {
        return effectiveUntil;
    }

    public void setEffectiveUntil(String effectiveUntil) {
        this.effectiveUntil = effectiveUntil;
    }

    public Double getExpectedExtraCost() {
        return expectedExtraCost;
    }

    public void setExpectedExtraCost(Double expectedExtraCost) {
        this.expectedExtraCost = expectedExtraCost;
    }

    public Double getCostEstimatedByLandbridge() {
        return costEstimatedByLandbridge;
    }

    public void setCostEstimatedByLandbridge(Double costEstimatedByLandbridge) {
        this.costEstimatedByLandbridge = costEstimatedByLandbridge;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

