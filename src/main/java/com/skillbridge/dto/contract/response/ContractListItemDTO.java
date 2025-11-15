package com.skillbridge.dto.contract.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * DTO for contract list item
 */
public class ContractListItemDTO {
    
    @JsonProperty("internalId")
    private Integer internalId; // Primary key (database ID) - used for navigation
    
    private String id; // Display ID format: CT-YYYY-NN or MSA-YYYY-NN (for display only)
    
    private Integer no; // Sequential number
    
    private String contractName;
    
    private String type; // "MSA" or "SOW"
    
    private String periodStart; // Format: YYYY/MM/DD or null
    
    private String periodEnd; // Format: YYYY/MM/DD or null
    
    private String period; // Formatted period: "YYYY/MM/DD-YYYY/MM/DD" or "-"
    
    private BigDecimal value; // Contract value in JPY or null
    
    private String formattedValue; // Formatted value: "¥X,XXX,XXX" or "-"
    
    private String status; // "Active", "Draft", "Pending", "Under Review", etc.
    
    private String assignee; // Assignee ID or null
    
    private String createdAt; // ISO format

    // Getters and Setters
    public Integer getInternalId() {
        return internalId;
    }

    public void setInternalId(Integer internalId) {
        this.internalId = internalId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getNo() {
        return no;
    }

    public void setNo(Integer no) {
        this.no = no;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(String periodStart) {
        this.periodStart = periodStart;
    }

    public String getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(String periodEnd) {
        this.periodEnd = periodEnd;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getFormattedValue() {
        return formattedValue;
    }

    public void setFormattedValue(String formattedValue) {
        this.formattedValue = formattedValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

