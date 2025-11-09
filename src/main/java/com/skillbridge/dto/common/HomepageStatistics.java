package com.skillbridge.dto.common;

public class HomepageStatistics {
    private Long totalEngineers;
    private Long totalCustomers;

    public HomepageStatistics() {
    }

    public HomepageStatistics(Long totalEngineers, Long totalCustomers) {
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
}

