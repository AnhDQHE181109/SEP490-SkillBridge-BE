package com.skillbridge.dto.contract.request;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create Change Request Request DTO
 * Data transfer object for creating a change request
 */
public class CreateChangeRequestRequest {
    private String title;
    private String type;
    private String description;
    private String reason;
    private LocalDate desiredStartDate;
    private LocalDate desiredEndDate;
    private BigDecimal expectedExtraCost;

    // Constructors
    public CreateChangeRequestRequest() {
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getDesiredStartDate() {
        return desiredStartDate;
    }

    public void setDesiredStartDate(LocalDate desiredStartDate) {
        this.desiredStartDate = desiredStartDate;
    }

    public LocalDate getDesiredEndDate() {
        return desiredEndDate;
    }

    public void setDesiredEndDate(LocalDate desiredEndDate) {
        this.desiredEndDate = desiredEndDate;
    }

    public BigDecimal getExpectedExtraCost() {
        return expectedExtraCost;
    }

    public void setExpectedExtraCost(BigDecimal expectedExtraCost) {
        this.expectedExtraCost = expectedExtraCost;
    }
}

