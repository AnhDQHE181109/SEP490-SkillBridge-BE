package com.skillbridge.dto.sales.response;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for current resources at a specific date
 * Used for CR creation/editing to show "Before" state
 */
public class CurrentResourcesDTO {
    private Integer sowContractId;
    private LocalDate asOfDate;
    private List<ResourceDTO> resources;

    public CurrentResourcesDTO() {
    }

    public CurrentResourcesDTO(Integer sowContractId, LocalDate asOfDate, List<ResourceDTO> resources) {
        this.sowContractId = sowContractId;
        this.asOfDate = asOfDate;
        this.resources = resources;
    }

    // Getters and Setters
    public Integer getSowContractId() {
        return sowContractId;
    }

    public void setSowContractId(Integer sowContractId) {
        this.sowContractId = sowContractId;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(LocalDate asOfDate) {
        this.asOfDate = asOfDate;
    }

    public List<ResourceDTO> getResources() {
        return resources;
    }

    public void setResources(List<ResourceDTO> resources) {
        this.resources = resources;
    }

    /**
     * Resource DTO - represents a single engineer resource
     */
    public static class ResourceDTO {
        private Integer engineerId;        // Logical ID (from current state calculation)
        private Integer baseEngineerId;    // Baseline ID (optional, if from baseline)
        private String level;
        private String role;
        private String engineerLevelLabel; // "Middle Backend Engineer" (combined display)
        private LocalDate startDate;
        private LocalDate endDate;
        private java.math.BigDecimal rating;
        private java.math.BigDecimal unitRate;

        public ResourceDTO() {
        }

        public ResourceDTO(Integer engineerId, Integer baseEngineerId, String level, String role,
                          String engineerLevelLabel, LocalDate startDate, LocalDate endDate,
                          java.math.BigDecimal rating, java.math.BigDecimal unitRate) {
            this.engineerId = engineerId;
            this.baseEngineerId = baseEngineerId;
            this.level = level;
            this.role = role;
            this.engineerLevelLabel = engineerLevelLabel;
            this.startDate = startDate;
            this.endDate = endDate;
            this.rating = rating;
            this.unitRate = unitRate;
        }

        // Getters and Setters
        public Integer getEngineerId() {
            return engineerId;
        }

        public void setEngineerId(Integer engineerId) {
            this.engineerId = engineerId;
        }

        public Integer getBaseEngineerId() {
            return baseEngineerId;
        }

        public void setBaseEngineerId(Integer baseEngineerId) {
            this.baseEngineerId = baseEngineerId;
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

        public String getEngineerLevelLabel() {
            return engineerLevelLabel;
        }

        public void setEngineerLevelLabel(String engineerLevelLabel) {
            this.engineerLevelLabel = engineerLevelLabel;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public java.math.BigDecimal getRating() {
            return rating;
        }

        public void setRating(java.math.BigDecimal rating) {
            this.rating = rating;
        }

        public java.math.BigDecimal getUnitRate() {
            return unitRate;
        }

        public void setUnitRate(java.math.BigDecimal unitRate) {
            this.unitRate = unitRate;
        }
    }
}

