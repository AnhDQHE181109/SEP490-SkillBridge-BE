package com.skillbridge.dto.sales.response;

import java.util.List;

/**
 * Change Requests List Response DTO
 * Paginated response for change requests list
 */
public class ChangeRequestsListResponseDTO {
    private List<ChangeRequestListItemDTO> content;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;

    // Getters and Setters
    public List<ChangeRequestListItemDTO> getContent() {
        return content;
    }

    public void setContent(List<ChangeRequestListItemDTO> content) {
        this.content = content;
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}

