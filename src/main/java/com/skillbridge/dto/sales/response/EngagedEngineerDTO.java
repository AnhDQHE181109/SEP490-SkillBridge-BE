package com.skillbridge.dto.sales.response;

public class EngagedEngineerDTO {
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
