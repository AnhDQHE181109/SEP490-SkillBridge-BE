package com.skillbridge.dto.contract.response;

import java.util.List;

/**
 * Contract Detail DTO
 * Data transfer object for contract detail information
 */
public class ContractDetailDTO {
    private Integer internalId; // Primary key (database ID)
    private String id; // Display ID format: MSA-YYYY-MM-DD-NN (for display only)
    
    // Overview
    private String contractType; // "MSA" or "SOW"
    private String contractName;
    private String effectiveStart; // Format: YYYY/MM/DD
    private String effectiveEnd; // Format: YYYY/MM/DD
    private String status; // "Draft", "Active", "Pending", "Under Review", etc.
    
    // Commercial Terms
    private String currency; // e.g., "JPY", "USD"
    private String paymentTerms; // e.g., "Net", "Net 30"
    private String invoicingCycle; // e.g., "Monthly", "Weekly"
    private String billingDay; // e.g., "Last business day"
    private String taxWithholding; // e.g., "10%"
    
    // Legal / Compliance
    private String ipOwnership; // e.g., "Client", "LandBridge"
    private String governingLaw; // e.g., "JP", "US"
    
    // Contacts
    private ContactInfo clientContact;
    private ContactInfo landbridgeContact;
    
    // SOW specific fields
    private String engagementType; // "Fixed Price" or "Retainer"
    private ParentMSAInfo parentMSA;
    private String projectName;
    private String scopeSummary;
    
    // Fixed Price SOW fields
    private List<MilestoneDeliverableDTO> milestones;
    private List<FixedPriceBillingDetailDTO> billingDetails;
    
    // Retainer SOW fields
    private List<DeliveryItemDTO> deliveryItems;
    private List<RetainerBillingDetailDTO> retainerBillingDetails;
    private List<EngagedEngineerDTO> engagedEngineers; // For Retainer
    
    // Change Requests (common for both)
    private List<ChangeRequestDTO> changeRequests;
    
    // Attachments
    private List<AttachmentDTO> attachments;
    
    // History
    private List<ContractHistoryItemDTO> history;
    
    // Constructors
    public ContractDetailDTO() {
    }

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

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
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

    public ContactInfo getClientContact() {
        return clientContact;
    }

    public void setClientContact(ContactInfo clientContact) {
        this.clientContact = clientContact;
    }

    public ContactInfo getLandbridgeContact() {
        return landbridgeContact;
    }

    public void setLandbridgeContact(ContactInfo landbridgeContact) {
        this.landbridgeContact = landbridgeContact;
    }

    public List<ContractHistoryItemDTO> getHistory() {
        return history;
    }

    public void setHistory(List<ContractHistoryItemDTO> history) {
        this.history = history;
    }

    // SOW specific getters and setters
    public String getEngagementType() {
        return engagementType;
    }

    public void setEngagementType(String engagementType) {
        this.engagementType = engagementType;
    }

    public ParentMSAInfo getParentMSA() {
        return parentMSA;
    }

    public void setParentMSA(ParentMSAInfo parentMSA) {
        this.parentMSA = parentMSA;
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

    public List<MilestoneDeliverableDTO> getMilestones() {
        return milestones;
    }

    public void setMilestones(List<MilestoneDeliverableDTO> milestones) {
        this.milestones = milestones;
    }

    public List<FixedPriceBillingDetailDTO> getBillingDetails() {
        return billingDetails;
    }

    public void setBillingDetails(List<FixedPriceBillingDetailDTO> billingDetails) {
        this.billingDetails = billingDetails;
    }

    public List<DeliveryItemDTO> getDeliveryItems() {
        return deliveryItems;
    }

    public void setDeliveryItems(List<DeliveryItemDTO> deliveryItems) {
        this.deliveryItems = deliveryItems;
    }

    public List<RetainerBillingDetailDTO> getRetainerBillingDetails() {
        return retainerBillingDetails;
    }

    public void setRetainerBillingDetails(List<RetainerBillingDetailDTO> retainerBillingDetails) {
        this.retainerBillingDetails = retainerBillingDetails;
    }

    public List<EngagedEngineerDTO> getEngagedEngineers() {
        return engagedEngineers;
    }

    public void setEngagedEngineers(List<EngagedEngineerDTO> engagedEngineers) {
        this.engagedEngineers = engagedEngineers;
    }

    public List<ChangeRequestDTO> getChangeRequests() {
        return changeRequests;
    }

    public void setChangeRequests(List<ChangeRequestDTO> changeRequests) {
        this.changeRequests = changeRequests;
    }

    public List<AttachmentDTO> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentDTO> attachments) {
        this.attachments = attachments;
    }

    /**
     * Attachment DTO
     * Nested class for attachment information
     */
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

    /**
     * Contact Info DTO
     * Nested class for contact information
     */
    public static class ContactInfo {
        private String name;
        private String email;

        public ContactInfo() {
        }

        public ContactInfo(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    /**
     * Parent MSA Info DTO
     * Nested class for parent MSA information
     */
    public static class ParentMSAInfo {
        private String id; // Display ID format: MSA-YYYY-MM-DD-NN
        private String status; // "Active", "Draft", etc.

        public ParentMSAInfo() {
        }

        public ParentMSAInfo(String id, String status) {
            this.id = id;
            this.status = status;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * Engaged Engineer DTO (for Retainer SOW)
     */
    public static class EngagedEngineerDTO {
        private Integer id;
        private String engineerLevel;
        private String startDate; // Format: YYYY-MM-DD
        private String endDate; // Format: YYYY-MM-DD
        private String billingType; // "Monthly" or "Hourly"
        private Double hourlyRate; // For hourly billing
        private Double hours; // For hourly billing
        private Double subtotal; // For hourly billing: hourlyRate * hours
        private Double rating; // Percentage (0-100)
        private Double salary; // Currency amount (for monthly) or subtotal (for hourly)

        public EngagedEngineerDTO() {
        }

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
    }
}

