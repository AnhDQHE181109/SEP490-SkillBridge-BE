package com.skillbridge.dto.engineer.response;

import java.util.List;

public class EngineerSearchResponse {
    private List<EngineerProfile> results;
    private Long totalResults;
    private Integer currentPage;
    private Integer totalPages;
    private Integer pageSize;

    public EngineerSearchResponse() {
    }

    public EngineerSearchResponse(List<EngineerProfile> results, Long totalResults, Integer currentPage, Integer totalPages, Integer pageSize) {
        this.results = results;
        this.totalResults = totalResults;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.pageSize = pageSize;
    }

    // Getters and Setters
    public List<EngineerProfile> getResults() {
        return results;
    }

    public void setResults(List<EngineerProfile> results) {
        this.results = results;
    }

    public Long getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(Long totalResults) {
        this.totalResults = totalResults;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}

