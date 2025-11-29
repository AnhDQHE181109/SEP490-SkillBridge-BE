package com.skillbridge.entity.contract;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CurrentEngineerState {

    private Integer engineerId;
    private String role;
    private String level;
    private BigDecimal rating;
    private BigDecimal unitRate;
    private LocalDate startDate;
    private LocalDate endDate;

    // Getters and Setters
    public Integer getEngineerId() { return engineerId; }
    public void setEngineerId(Integer engineerId) { this.engineerId = engineerId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public BigDecimal getUnitRate() { return unitRate; }
    public void setUnitRate(BigDecimal unitRate) { this.unitRate = unitRate; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

}
