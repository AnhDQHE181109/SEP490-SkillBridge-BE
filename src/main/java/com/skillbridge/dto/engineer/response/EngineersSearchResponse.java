package com.skillbridge.dto.engineer.response;

import java.util.List;

public class EngineersSearchResponse {

    private List<EngineerProfileDTO> engineersResults;
    private Long totalResults;
    private Integer currentPage;
    private Integer totalPages;
    private Integer pageSize;

    public EngineersSearchResponse() {}

    public EngineersSearchResponse(List<EngineerProfileDTO> engineersResults, Long totalResults, Integer currentPage, Integer totalPages, Integer pageSize) {
        this.engineersResults = engineersResults;
        this.totalResults = totalResults;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.pageSize = pageSize;
    }

    public List<EngineerProfileDTO> getEngineersResults() {
        return engineersResults;
    }

    public void setEngineersResults(List<EngineerProfileDTO> engineersResults) {
        this.engineersResults = engineersResults;
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
