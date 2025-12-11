package com.skillbridge.dto.sales.request;

import java.util.List;

/**
 * Create Change Request Request DTO for Sales Portal
 * Data transfer object for creating a change request for Retainer SOW contracts
 */
public class CreateChangeRequestRequest {
    private String title; // CR Title
    private String type; // CR Type: "Extend Schedule", "Rate Change", "Increase Resource", "Other"
    private String summary; // Change Summary
    private String effectiveFrom; // Format: YYYY-MM-DD (for Retainer)
    private String effectiveUntil; // Format: YYYY-MM-DD (for Retainer)
    private String references; // References (optional)
    private List<EngagedEngineerDTO> engagedEngineers; // Engaged engineers
    private List<BillingDetailDTO> billingDetails; // Billing details
    private ImpactAnalysisDTO impactAnalysis; // Impact analysis for Fixed Price
    private Integer internalReviewerId; // Internal reviewer user ID
    private String comment; // Comment (optional)
    private String action; // "save" or "submit"
    private String reviewAction; // "APPROVE" or "REQUEST_REVISION" (when internal reviewer is current user)

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

    public String getReferences() {
        return references;
    }

    public void setReferences(String references) {
        this.references = references;
    }

    public List<EngagedEngineerDTO> getEngagedEngineers() {
        return engagedEngineers;
    }

    public void setEngagedEngineers(List<EngagedEngineerDTO> engagedEngineers) {
        this.engagedEngineers = engagedEngineers;
    }

    public List<BillingDetailDTO> getBillingDetails() {
        return billingDetails;
    }

    public void setBillingDetails(List<BillingDetailDTO> billingDetails) {
        this.billingDetails = billingDetails;
    }

    public ImpactAnalysisDTO getImpactAnalysis() {
        return impactAnalysis;
    }

    public void setImpactAnalysis(ImpactAnalysisDTO impactAnalysis) {
        this.impactAnalysis = impactAnalysis;
    }

    public Integer getInternalReviewerId() {
        return internalReviewerId;
    }

    public void setInternalReviewerId(Integer internalReviewerId) {
        this.internalReviewerId = internalReviewerId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReviewAction() {
        return reviewAction;
    }

    public void setReviewAction(String reviewAction) {
        this.reviewAction = reviewAction;
    }

    // Inner DTOs
    public static class EngagedEngineerDTO {
        private Integer baseEngineerId; // Baseline engineer ID (for matching with baseline)
        private Integer engineerId; // Engineer ID (for matching with current engineers)
        private String engineerLevel;
        private String level; // Engineer level (separate field from payload)
        private String role; // Engineer role (separate field from payload)
        private String startDate; // Format: YYYY-MM-DD
        private String endDate; // Format: YYYY-MM-DD
        private String billingType; // "Monthly" or "Hourly"
        private Double hourlyRate; // For hourly billing
        private Double hours; // For hourly billing
        private Double subtotal; // For hourly billing: hourlyRate * hours
        private Double rating; // Percentage (0-100)
        private Double salary; // Currency amount (for monthly) or subtotal (for hourly)

        public String getEngineerLevel() {
            return engineerLevel;
        }

        public void setEngineerLevel(String engineerLevel) {
            this.engineerLevel = engineerLevel;
        }

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }

        public String getBillingType() {
            return billingType;
        }

        public void setBillingType(String billingType) {
            this.billingType = billingType;
        }

        public Double getHourlyRate() {
            return hourlyRate;
        }

        public void setHourlyRate(Double hourlyRate) {
            this.hourlyRate = hourlyRate;
        }

        public Double getHours() {
            return hours;
        }

        public void setHours(Double hours) {
            this.hours = hours;
        }

        public Double getSubtotal() {
            return subtotal;
        }

        public void setSubtotal(Double subtotal) {
            this.subtotal = subtotal;
        }

        public Double getRating() {
            return rating;
        }

        public void setRating(Double rating) {
            this.rating = rating;
        }

        public Double getSalary() {
            return salary;
        }

        public void setSalary(Double salary) {
            this.salary = salary;
        }

        public Integer getBaseEngineerId() {
            return baseEngineerId;
        }

        public void setBaseEngineerId(Integer baseEngineerId) {
            this.baseEngineerId = baseEngineerId;
        }

        public Integer getEngineerId() {
            return engineerId;
        }

        public void setEngineerId(Integer engineerId) {
            this.engineerId = engineerId;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class BillingDetailDTO {
        private String paymentDate; // Format: YYYY-MM-DD
        private String deliveryNote;
        private Double amount; // Currency amount

        public String getPaymentDate() {
            return paymentDate;
        }

        public void setPaymentDate(String paymentDate) {
            this.paymentDate = paymentDate;
        }

        public String getDeliveryNote() {
            return deliveryNote;
        }

        public void setDeliveryNote(String deliveryNote) {
            this.deliveryNote = deliveryNote;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }
    }

    public static class ImpactAnalysisDTO {
        private Integer devHours;
        private Integer testHours;
        private String newEndDate; // Format: YYYY-MM-DD
        private Integer delayDuration; // in days
        private Double additionalCost; // Currency amount

        public Integer getDevHours() {
            return devHours;
        }

        public void setDevHours(Integer devHours) {
            this.devHours = devHours;
        }

        public Integer getTestHours() {
            return testHours;
        }

        public void setTestHours(Integer testHours) {
            this.testHours = testHours;
        }

        public String getNewEndDate() {
            return newEndDate;
        }

        public void setNewEndDate(String newEndDate) {
            this.newEndDate = newEndDate;
        }

        public Integer getDelayDuration() {
            return delayDuration;
        }

        public void setDelayDuration(Integer delayDuration) {
            this.delayDuration = delayDuration;
        }

        public Double getAdditionalCost() {
            return additionalCost;
        }

        public void setAdditionalCost(Double additionalCost) {
            this.additionalCost = additionalCost;
        }
    }
}

