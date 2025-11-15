package com.skillbridge.dto.contract.response;

import java.util.List;

/**
 * Change Request Detail DTO
 * Data transfer object for change request detail information
 */
public class ChangeRequestDetailDTO {
    private Integer id;
    private String changeRequestId; // Format: CR-YYYY-NN
    private String title;
    private String type;
    private String description;
    private String reason;
    private String status;
    private String createdBy;
    private String createdDate;
    private String desiredStartDate;
    private String desiredEndDate;
    private String expectedExtraCost;
    private List<EvidenceItemDTO> evidence;
    private List<AttachmentDTO> attachments;
    private List<HistoryItemDTO> history;
    private ImpactAnalysisDTO impactAnalysis;

    // Constructors
    public ChangeRequestDetailDTO() {
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

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getDesiredStartDate() {
        return desiredStartDate;
    }

    public void setDesiredStartDate(String desiredStartDate) {
        this.desiredStartDate = desiredStartDate;
    }

    public String getDesiredEndDate() {
        return desiredEndDate;
    }

    public void setDesiredEndDate(String desiredEndDate) {
        this.desiredEndDate = desiredEndDate;
    }

    public String getExpectedExtraCost() {
        return expectedExtraCost;
    }

    public void setExpectedExtraCost(String expectedExtraCost) {
        this.expectedExtraCost = expectedExtraCost;
    }

    public List<EvidenceItemDTO> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<EvidenceItemDTO> evidence) {
        this.evidence = evidence;
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

    public ImpactAnalysisDTO getImpactAnalysis() {
        return impactAnalysis;
    }

    public void setImpactAnalysis(ImpactAnalysisDTO impactAnalysis) {
        this.impactAnalysis = impactAnalysis;
    }

    /**
     * Evidence Item DTO
     */
    public static class EvidenceItemDTO {
        private String type; // "link" or "file"
        private String value;

        public EvidenceItemDTO() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * Attachment DTO
     */
    public static class AttachmentDTO {
        private Integer id;
        private String fileName;
        private String filePath;
        private Long fileSize;
        private String uploadedAt;

        public AttachmentDTO() {
        }

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

        public String getUploadedAt() {
            return uploadedAt;
        }

        public void setUploadedAt(String uploadedAt) {
            this.uploadedAt = uploadedAt;
        }
    }

    /**
     * History Item DTO
     */
    public static class HistoryItemDTO {
        private Integer id;
        private String action;
        private String userName;
        private String timestamp;

        public HistoryItemDTO() {
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }

    /**
     * Impact Analysis DTO (can be Fixed Price or Retainer)
     */
    public static class ImpactAnalysisDTO {
        // For Fixed Price
        private Integer devHours;
        private Integer testHours;
        private String newEndDate;
        private Integer delayDuration;
        private String additionalCost;

        // For Retainer
        private List<EngagedEngineerDTO> engagedEngineers;
        private List<BillingDetailDTO> billingDetails;

        public ImpactAnalysisDTO() {
        }

        // Getters and Setters for Fixed Price
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

        public String getAdditionalCost() {
            return additionalCost;
        }

        public void setAdditionalCost(String additionalCost) {
            this.additionalCost = additionalCost;
        }

        // Getters and Setters for Retainer
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

        /**
         * Engaged Engineer DTO (for Retainer)
         */
        public static class EngagedEngineerDTO {
            private String engineerLevel;
            private String startDate;
            private String endDate;
            private String rating; // percentage
            private String salary;

            public EngagedEngineerDTO() {
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

            public String getRating() {
                return rating;
            }

            public void setRating(String rating) {
                this.rating = rating;
            }

            public String getSalary() {
                return salary;
            }

            public void setSalary(String salary) {
                this.salary = salary;
            }
        }

        /**
         * Billing Detail DTO (for Retainer)
         */
        public static class BillingDetailDTO {
            private String paymentDate;
            private String deliveryNote;
            private String amount;

            public BillingDetailDTO() {
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

            public String getAmount() {
                return amount;
            }

            public void setAmount(String amount) {
                this.amount = amount;
            }
        }
    }
}

