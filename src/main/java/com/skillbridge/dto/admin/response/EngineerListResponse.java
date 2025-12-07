package com.skillbridge.dto.admin.response;

import java.util.List;

/**
 * Engineer List Response DTO
 * Response DTO for paginated engineer list in Admin Engineer List
 */
public class EngineerListResponse {
    private List<EngineerResponseDTO> content;
    private PageInfo page;

    // Constructors
    public EngineerListResponse() {
    }

    public EngineerListResponse(List<EngineerResponseDTO> content, PageInfo page) {
        this.content = content;
        this.page = page;
    }

    // Getters and Setters
    public List<EngineerResponseDTO> getContent() {
        return content;
    }

    public void setContent(List<EngineerResponseDTO> content) {
        this.content = content;
    }

    public PageInfo getPage() {
        return page;
    }

    public void setPage(PageInfo page) {
        this.page = page;
    }

    /**
     * Page Information Inner Class
     */
    public static class PageInfo {
        private long totalElements;
        private int totalPages;
        private int currentPage;
        private int pageSize;

        // Constructors
        public PageInfo() {
        }

        public PageInfo(long totalElements, int totalPages, int currentPage, int pageSize) {
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.currentPage = currentPage;
            this.pageSize = pageSize;
        }

        // Getters and Setters
        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
    }
}

