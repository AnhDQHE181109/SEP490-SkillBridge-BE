package com.skillbridge.dto;

public class HomepageStatisticsDTO {

    private Long totalEngineers;
    private Long totalCustomers;

    public HomepageStatisticsDTO(Long totalEngineers, Long totalCustomers) {
        this.totalEngineers = totalEngineers;
        this.totalCustomers = totalCustomers;
    }

    public Long getTotalEngineers() {
        return totalEngineers;
    }

    public void setTotalEngineers(Long totalEngineers) {
        this.totalEngineers = totalEngineers;
    }

    public Long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(Long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    @Override
    public String toString() {
        return "HomepageDTO{" +
                "totalEngineers=" + totalEngineers +
                ", totalCustomers=" + totalCustomers +
                '}';
    }
}
