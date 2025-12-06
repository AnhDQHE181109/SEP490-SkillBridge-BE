package com.skillbridge.dto.sales.response;

import java.util.List;

/**
 * Sales Change Request Detail DTO
 * Data transfer object for change request detail information in Sales Portal
 */
public class SalesChangeRequestDetailDTO {
    private Integer id;
    private String changeRequestId; // Format: CR-YYYY-NN
    private String title;
    private String type;
    private String summary;
    private String effectiveFrom; // Format: YYYY-MM-DD
    private String effectiveUntil; // Format: YYYY-MM-DD
    private String references;
    private String status;
    private String createdBy;
    private Integer createdById; // Add createdBy ID for frontend to check permissions
    private String createdDate; // Format: YYYY-MM-DD
    private List<EngagedEngineerDTO> engagedEngineers;
    private List<BillingDetailDTO> billingDetails;
    private List<AttachmentDTO> attachments;
    private List<HistoryItemDTO> history;
    private Integer internalReviewerId;
    private String internalReviewerName;
    private String reviewNotes;
    private String reviewAction;
    private String reviewDate;
    private String comment;
    // Impact Analysis fields (for Fixed Price SOW)
    private Integer devHours;
    private Integer testHours;
    private String newEndDate; // Format: YYYY-MM-DD
    private Integer delayDuration; // in days
    private Double additionalCost; // Currency amount

    // Constructors
    public SalesChangeRequestDetailDTO() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Integer createdById) {
        this.createdById = createdById;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
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

    public List<HistoryItemDTO> getHistory() {
        return history;
    }

    public void setHistory(List<HistoryItemDTO> history) {
        this.history = history;
    }

    public Integer getInternalReviewerId() {
        return internalReviewerId;
    }

    public void setInternalReviewerId(Integer internalReviewerId) {
        this.internalReviewerId = internalReviewerId;
    }

    public String getInternalReviewerName() {
        return internalReviewerName;
    }

    public void setInternalReviewerName(String internalReviewerName) {
        this.internalReviewerName = internalReviewerName;
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

    public String getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(String reviewDate) {
        this.reviewDate = reviewDate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

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

    public static class AttachmentDTO {
        private Integer id;
        private String fileName;
        private String filePath;
        private Long fileSize;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }
    }

}

