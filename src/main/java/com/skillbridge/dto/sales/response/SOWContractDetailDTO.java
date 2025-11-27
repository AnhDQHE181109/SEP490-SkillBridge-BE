package com.skillbridge.dto.sales.response;

import java.util.List;

/**
 * Detail DTO for SOW contract
 */
public class SOWContractDetailDTO {
    private Integer id;
    private String contractId; // Format: SOW-YYYY-MM-DD-NN
    private String contractName;
    private String status;
    
    // SOW Summary
    private String msaId; // Parent MSA ID (format: MSA-YYYY-NN)
    private Integer clientId;
    private String clientName;
    private String clientEmail;
    private String effectiveStart; // Format: YYYY-MM-DD
    private String effectiveEnd; // Format: YYYY-MM-DD
    private Integer assigneeUserId;
    private String assigneeName;
    private String projectName;
    private String scopeSummary;
    private String engagementType; // "Fixed Price" or "Retainer"
    private Double value; // Total contract value
    
    // Commercial Terms
    private String currency;
    private String paymentTerms;
    private String invoicingCycle;
    private String billingDay;
    private String taxWithholding;
    
    // Legal / Compliance
    private String ipOwnership;
    private String governingLaw;
    
    // Contacts
    private Integer clientContactId;
    private String clientContactName;
    private String clientContactEmail;
    private Integer landbridgeContactId;
    private String landbridgeContactName;
    private String landbridgeContactEmail;
    
    // SOW specific data
    private List<MilestoneDeliverableDTO> milestoneDeliverables; // For Fixed Price
    private List<DeliveryItemDTO> deliveryItems; // For Retainer (deprecated)
    private List<EngagedEngineerDTO> engagedEngineers; // For Retainer
    private List<BillingDetailDTO> billingDetails;
    
    // Attachments
    private List<AttachmentDTO> attachments;
    
    // Review
    private Integer reviewerId;
    private String reviewerName;
    private String reviewNotes;
    private String reviewAction;
    
    // History
    private List<HistoryItemDTO> history;
    
    // Versioning (for Retainer SOW)
    private Integer version; // Version number (V1, V2, V3, etc.)

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
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

    public Integer getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(Integer assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getScopeSummary() {
        return scopeSummary;
    }

    public void setScopeSummary(String scopeSummary) {
        this.scopeSummary = scopeSummary;
    }

    public String getEngagementType() {
        return engagementType;
    }

    public void setEngagementType(String engagementType) {
        this.engagementType = engagementType;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public String getInvoicingCycle() {
        return invoicingCycle;
    }

    public void setInvoicingCycle(String invoicingCycle) {
        this.invoicingCycle = invoicingCycle;
    }

    public String getBillingDay() {
        return billingDay;
    }

    public void setBillingDay(String billingDay) {
        this.billingDay = billingDay;
    }

    public String getTaxWithholding() {
        return taxWithholding;
    }

    public void setTaxWithholding(String taxWithholding) {
        this.taxWithholding = taxWithholding;
    }

    public String getIpOwnership() {
        return ipOwnership;
    }

    public void setIpOwnership(String ipOwnership) {
        this.ipOwnership = ipOwnership;
    }

    public String getGoverningLaw() {
        return governingLaw;
    }

    public void setGoverningLaw(String governingLaw) {
        this.governingLaw = governingLaw;
    }

    public Integer getClientContactId() {
        return clientContactId;
    }

    public void setClientContactId(Integer clientContactId) {
        this.clientContactId = clientContactId;
    }

    public String getClientContactName() {
        return clientContactName;
    }

    public void setClientContactName(String clientContactName) {
        this.clientContactName = clientContactName;
    }

    public String getClientContactEmail() {
        return clientContactEmail;
    }

    public void setClientContactEmail(String clientContactEmail) {
        this.clientContactEmail = clientContactEmail;
    }

    public Integer getLandbridgeContactId() {
        return landbridgeContactId;
    }

    public void setLandbridgeContactId(Integer landbridgeContactId) {
        this.landbridgeContactId = landbridgeContactId;
    }

    public String getLandbridgeContactName() {
        return landbridgeContactName;
    }

    public void setLandbridgeContactName(String landbridgeContactName) {
        this.landbridgeContactName = landbridgeContactName;
    }

    public String getLandbridgeContactEmail() {
        return landbridgeContactEmail;
    }

    public void setLandbridgeContactEmail(String landbridgeContactEmail) {
        this.landbridgeContactEmail = landbridgeContactEmail;
    }

    public List<MilestoneDeliverableDTO> getMilestoneDeliverables() {
        return milestoneDeliverables;
    }

    public void setMilestoneDeliverables(List<MilestoneDeliverableDTO> milestoneDeliverables) {
        this.milestoneDeliverables = milestoneDeliverables;
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

    public List<BillingDetailDTO> getBillingDetails() {
        return billingDetails;
    }

    public void setBillingDetails(List<BillingDetailDTO> billingDetails) {
        this.billingDetails = billingDetails;
    }

    public List<AttachmentDTO> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentDTO> attachments) {
        this.attachments = attachments;
    }

    public Integer getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Integer reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
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

    public List<HistoryItemDTO> getHistory() {
        return history;
    }

    public void setHistory(List<HistoryItemDTO> history) {
        this.history = history;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    // Inner DTOs
    public static class MilestoneDeliverableDTO {
        private Integer id;
        private String milestone;
        private String deliveryNote;
        private String acceptanceCriteria;
        private String plannedEnd; // Format: YYYY-MM-DD
        private Double paymentPercentage;

        // Getters and Setters
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

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

    public static class DeliveryItemDTO {
        private Integer id;
        private String milestone;
        private String deliveryNote;
        private Double amount;
        private String paymentDate; // Format: YYYY-MM-DD

        // Getters and Setters
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

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

    public static class EngagedEngineerDTO {
        private Integer id;
        private String engineerLevel;
        private String startDate; // Format: YYYY-MM-DD
        private String endDate; // Format: YYYY-MM-DD
        private Double rating; // Percentage (0-100)
        private Double salary; // Currency amount

        // Getters and Setters
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

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

    public static class BillingDetailDTO {
        private Integer id;
        private String billingName;
        private String milestone;
        private Double amount;
        private Double percentage;
        private String invoiceDate; // Format: YYYY-MM-DD (for Fixed Price) or paymentDate (for Retainer)
        private String deliveryNote;

        // Getters and Setters
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

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

        public String getInvoiceDate() {
            return invoiceDate;
        }

        public void setInvoiceDate(String invoiceDate) {
            this.invoiceDate = invoiceDate;
        }

        public String getDeliveryNote() {
            return deliveryNote;
        }

        public void setDeliveryNote(String deliveryNote) {
            this.deliveryNote = deliveryNote;
        }
    }

    public static class AttachmentDTO {
        private String s3Key;
        private String fileName;
        private Long fileSize;

        public AttachmentDTO() {
        }

        public AttachmentDTO(String s3Key, String fileName, Long fileSize) {
            this.s3Key = s3Key;
            this.fileName = fileName;
            this.fileSize = fileSize;
        }

        public String getS3Key() {
            return s3Key;
        }

        public void setS3Key(String s3Key) {
            this.s3Key = s3Key;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }
    }

    public static class HistoryItemDTO {
        private Integer id;
        private String date; // Format: YYYY-MM-DD
        private String description;
        private String documentLink;
        private String documentName;

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

        public String getDocumentLink() {
            return documentLink;
        }

        public void setDocumentLink(String documentLink) {
            this.documentLink = documentLink;
        }

        public String getDocumentName() {
            return documentName;
        }

        public void setDocumentName(String documentName) {
            this.documentName = documentName;
        }
    }
}

