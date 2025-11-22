package com.skillbridge.dto.sales.response;

import java.util.List;

/**
 * DTO for opportunities list response
 */
public class OpportunitiesListResponse {
    private List<OpportunityListItemDTO> opportunities;
    private long total;
    private int page;
    private int pageSize;
    private int totalPages;

    public OpportunitiesListResponse() {
    }

    public OpportunitiesListResponse(List<OpportunityListItemDTO> opportunities, long total, int page, int pageSize, int totalPages) {
        this.opportunities = opportunities;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
    }

    // Getters and Setters
    public List<OpportunityListItemDTO> getOpportunities() {
        return opportunities;
    }

    public void setOpportunities(List<OpportunityListItemDTO> opportunities) {
        this.opportunities = opportunities;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

