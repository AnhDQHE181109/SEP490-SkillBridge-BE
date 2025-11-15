package com.skillbridge.dto.contract.response;

/**
 * Change Request DTO
 * Data transfer object for change request information
 */
public class ChangeRequestDTO {
    private Integer id;
    private String changeRequestId; // Format: CR-YYYY-NN
    private String type; // "Add Scope", "Extend", "Reduce"
    private String summary;
    private String plannedEnd; // Format: YYYY/MM/DD (for Fixed Price)
    private String effectiveFrom; // Format: YYYY/MM/DD (for Retainer)
    private String effectiveUntil; // Format: YYYY/MM/DD (for Retainer)
    private String amount; // Currency format
    private String status; // "Approved", "Pending", "Rejected"

    // Constructors
    public ChangeRequestDTO() {
    }

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

    public String getPlannedEnd() {
        return plannedEnd;
    }

    public void setPlannedEnd(String plannedEnd) {
        this.plannedEnd = plannedEnd;
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

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

