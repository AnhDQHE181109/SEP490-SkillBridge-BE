package com.skillbridge.dto.sales.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Sales Dashboard Recent Client Activities DTO
 */
public class SalesDashboardActivitiesDTO {

    @JsonProperty("activities")
    private List<ActivityItem> activities;

    @JsonProperty("total")
    private Integer total;

    public SalesDashboardActivitiesDTO() {
    }

    public List<ActivityItem> getActivities() {
        return activities;
    }

    public void setActivities(List<ActivityItem> activities) {
        this.activities = activities;
        this.total = activities != null ? activities.size() : 0;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public static class ActivityItem {
        @JsonProperty("id")
        private Integer id;

        @JsonProperty("description")
        private String description; // e.g., "CR-020 submitted by Client A"

        @JsonProperty("timeAgo")
        private String timeAgo; // e.g., "2 min ago", "1 hour ago"

        @JsonProperty("timestamp")
        private String timestamp; // ISO format: "2025-11-22T10:30:00Z"

        @JsonProperty("entityType")
        private String entityType; // "CHANGE_REQUEST", "CONTRACT", "PROPOSAL", "CONTACT"

        @JsonProperty("entityId")
        private Integer entityId;

        @JsonProperty("clientName")
        private String clientName;

        public ActivityItem() {
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTimeAgo() {
            return timeAgo;
        }

        public void setTimeAgo(String timeAgo) {
            this.timeAgo = timeAgo;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getEntityType() {
            return entityType;
        }

        public void setEntityType(String entityType) {
            this.entityType = entityType;
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
    }
}

