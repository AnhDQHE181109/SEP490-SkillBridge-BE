package com.skillbridge.dto.dashboard.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Alerts Response DTO
 * Contains list of alerts/notifications
 */
public class AlertsResponseDTO {

    @JsonProperty("alerts")
    private List<AlertDTO> alerts;

    @JsonProperty("total")
    private Integer total;

    // Constructors
    public AlertsResponseDTO() {
    }

    public AlertsResponseDTO(List<AlertDTO> alerts, Integer total) {
        this.alerts = alerts;
        this.total = total;
    }

    // Getters and Setters
    public List<AlertDTO> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<AlertDTO> alerts) {
        this.alerts = alerts;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}

