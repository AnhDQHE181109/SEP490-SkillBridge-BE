package com.skillbridge.dto.sales.response;

import java.util.List;

/**
 * Detail DTO for MSA contract
 */
public class MSAContractDetailDTO {
    private Integer id;
    private String contractId; // Format: MSA-YYYY-NN
    private String contractName;
    private String status;
    
    // MSA Summary
    private String opportunityId;
    private Integer clientId;
    private String clientName;
    private String clientEmail;
    private String effectiveStart; // Format: YYYY-MM-DD
    private String effectiveEnd; // Format: YYYY-MM-DD
    private Integer assigneeUserId;
    private String assigneeName;
    private String note;
    
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
    
    // Attachments
    private List<AttachmentDTO> attachments;
    
    // Review
    private Integer reviewerId;
    private String reviewerName;
    private String reviewNotes;
    private String reviewAction;
    
    // History
    private List<HistoryItemDTO> history;

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

    public String getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(String opportunityId) {
        this.opportunityId = opportunityId;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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

    /**
     * History Item DTO
     */
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

    /**
     * Attachment DTO
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
}

