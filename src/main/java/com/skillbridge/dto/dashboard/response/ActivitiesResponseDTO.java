package com.skillbridge.dto.dashboard.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Activities Response DTO
 * Contains list of recent activities
 */
public class ActivitiesResponseDTO {

    @JsonProperty("activities")
    private List<ActivityDTO> activities;

    @JsonProperty("total")
    private Integer total;

    // Constructors
    public ActivitiesResponseDTO() {
    }

    public ActivitiesResponseDTO(List<ActivityDTO> activities, Integer total) {
        this.activities = activities;
        this.total = total;
    }

    // Getters and Setters
    public List<ActivityDTO> getActivities() {
        return activities;
    }

    public void setActivities(List<ActivityDTO> activities) {
        this.activities = activities;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}

