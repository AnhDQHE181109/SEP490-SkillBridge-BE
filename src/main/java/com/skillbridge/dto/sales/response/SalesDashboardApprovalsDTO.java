package com.skillbridge.dto.sales.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Sales Dashboard Approvals Waiting from Client DTO
 */
public class SalesDashboardApprovalsDTO {

    @JsonProperty("approvals")
    private List<ApprovalItem> approvals;

    @JsonProperty("total")
    private Integer total;

    public SalesDashboardApprovalsDTO() {
    }

    public List<ApprovalItem> getApprovals() {
        return approvals;
    }

    public void setApprovals(List<ApprovalItem> approvals) {
        this.approvals = approvals;
        this.total = approvals != null ? approvals.size() : 0;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public static class ApprovalItem {
        @JsonProperty("id")
        private Integer id;

        @JsonProperty("entityType")
        private String entityType; // "PROPOSAL" or "SOW" or "MSA"

        @JsonProperty("entityNumber")
        private String entityNumber; // e.g., "P-103", "SW-021"

        @JsonProperty("entityId")
        private Integer entityId;

        @JsonProperty("clientName")
        private String clientName;

        @JsonProperty("status")
        private String status;

        @JsonProperty("sentDate")
        private String sentDate; // Format: "YYYY-MM-DD"

        @JsonProperty("description")
        private String description; // e.g., "Proposal #P-103 - ACME JP (Sent 10 Oct)"

        public ApprovalItem() {
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getEntityType() {
            return entityType;
        }

        public void setEntityType(String entityType) {
            this.entityType = entityType;
        }

        public String getEntityNumber() {
            return entityNumber;
        }

        public void setEntityNumber(String entityNumber) {
            this.entityNumber = entityNumber;
        }

        public Integer getEntityId() {
            return entityId;
        }

        public void setEntityId(Integer entityId) {
            this.entityId = entityId;
        }

        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getSentDate() {
            return sentDate;
        }

        public void setSentDate(String sentDate) {
            this.sentDate = sentDate;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}

