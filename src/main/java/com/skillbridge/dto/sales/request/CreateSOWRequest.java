package com.skillbridge.dto.sales.request;

import java.util.List;

/**
 * Request DTO for creating SOW contract
 */
public class CreateSOWRequest {
    private String msaId; // Format: MSA-YYYY-NN
    private Integer clientId;
    private String engagementType; // "Fixed Price" or "Retainer"
    private String effectiveStart; // Format: YYYY-MM-DD
    private String effectiveEnd; // Format: YYYY-MM-DD
    private String status;
    private Integer assigneeUserId;
    private String note;
    private String scopeSummary;
    private String projectName;
    private Double contractValue; // Total contract value for Fixed Price
    private List<DeliveryItemDTO> deliveryItems; // For Retainer (deprecated, use engagedEngineers instead)
    private List<EngagedEngineerDTO> engagedEngineers; // For Retainer
    private List<MilestoneDeliverableDTO> milestoneDeliverables; // For Fixed Price
    private List<BillingDetailDTO> billingDetails; // Manual input (no longer auto-generated)
    private Integer reviewerId;
    private String reviewNotes;
    private String reviewAction;

    // Getters and Setters
    public String getMsaId() {
        return msaId;
    }

    public void setMsaId(String msaId) {
        this.msaId = msaId;
    }

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public String getEngagementType() {
        return engagementType;
    }

    public void setEngagementType(String engagementType) {
        this.engagementType = engagementType;
    }

    public String getEffectiveStart() {
        return effectiveStart;
    }

    public void setEffectiveStart(String effectiveStart) {
        this.effectiveStart = effectiveStart;
    }

    public String getEffectiveEnd() {
        return effectiveEnd;
    }

    public void setEffectiveEnd(String effectiveEnd) {
        this.effectiveEnd = effectiveEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(Integer assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getScopeSummary() {
        return scopeSummary;
    }

    public void setScopeSummary(String scopeSummary) {
        this.scopeSummary = scopeSummary;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Double getContractValue() {
        return contractValue;
    }

    public void setContractValue(Double contractValue) {
        this.contractValue = contractValue;
    }

    public List<DeliveryItemDTO> getDeliveryItems() {
        return deliveryItems;
    }

    public void setDeliveryItems(List<DeliveryItemDTO> deliveryItems) {
        this.deliveryItems = deliveryItems;
    }

    public List<EngagedEngineerDTO> getEngagedEngineers() {
        return engagedEngineers;
    }

    public void setEngagedEngineers(List<EngagedEngineerDTO> engagedEngineers) {
        this.engagedEngineers = engagedEngineers;
    }

    public List<MilestoneDeliverableDTO> getMilestoneDeliverables() {
        return milestoneDeliverables;
    }

    public void setMilestoneDeliverables(List<MilestoneDeliverableDTO> milestoneDeliverables) {
        this.milestoneDeliverables = milestoneDeliverables;
    }

    public List<BillingDetailDTO> getBillingDetails() {
        return billingDetails;
    }

    public void setBillingDetails(List<BillingDetailDTO> billingDetails) {
        this.billingDetails = billingDetails;
    }

    public Integer getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Integer reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public String getReviewAction() {
        return reviewAction;
    }

    public void setReviewAction(String reviewAction) {
        this.reviewAction = reviewAction;
    }

    // Inner DTOs
    public static class DeliveryItemDTO {
        private String milestone;
        private String deliveryNote;
        private Double amount;
        private String paymentDate; // Format: YYYY-MM-DD

        public String getMilestone() {
            return milestone;
        }

        public void setMilestone(String milestone) {
            this.milestone = milestone;
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

        public String getPaymentDate() {
            return paymentDate;
        }

        public void setPaymentDate(String paymentDate) {
            this.paymentDate = paymentDate;
        }
    }

    public static class MilestoneDeliverableDTO {
        private String milestone;
        private String deliveryNote;
        private String acceptanceCriteria;
        private String plannedEnd; // Format: YYYY-MM-DD
        private Double paymentPercentage;

        public String getMilestone() {
            return milestone;
        }

        public void setMilestone(String milestone) {
            this.milestone = milestone;
        }

        public String getDeliveryNote() {
            return deliveryNote;
        }

        public void setDeliveryNote(String deliveryNote) {
            this.deliveryNote = deliveryNote;
        }

        public String getAcceptanceCriteria() {
            return acceptanceCriteria;
        }

        public void setAcceptanceCriteria(String acceptanceCriteria) {
            this.acceptanceCriteria = acceptanceCriteria;
        }

        public String getPlannedEnd() {
            return plannedEnd;
        }

        public void setPlannedEnd(String plannedEnd) {
            this.plannedEnd = plannedEnd;
        }

        public Double getPaymentPercentage() {
            return paymentPercentage;
        }

        public void setPaymentPercentage(Double paymentPercentage) {
            this.paymentPercentage = paymentPercentage;
        }
    }

    public static class BillingDetailDTO {
        private String billingName; // For Fixed Price
        private String milestone; // For Fixed Price
        private String paymentDate; // Format: YYYY-MM-DD (for Retainer) or Invoice Date (for Fixed Price)
        private String deliveryNote;
        private Double amount;
        private Double percentage; // For Fixed Price

        public String getBillingName() {
            return billingName;
        }

        public void setBillingName(String billingName) {
            this.billingName = billingName;
        }

        public String getMilestone() {
            return milestone;
        }

        public void setMilestone(String milestone) {
            this.milestone = milestone;
        }

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

        public Double getPercentage() {
            return percentage;
        }

        public void setPercentage(Double percentage) {
            this.percentage = percentage;
        }
    }

    public static class EngagedEngineerDTO {
        private String engineerLevel;
        private String startDate; // Format: YYYY-MM-DD
        private String endDate; // Format: YYYY-MM-DD
        private Double rating; // Percentage (0-100)
        private Double salary; // Currency amount

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
    }
}

