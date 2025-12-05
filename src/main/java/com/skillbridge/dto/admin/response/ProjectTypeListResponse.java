package com.skillbridge.dto.admin.response;

import java.util.List;

/**
 * Project Type List Response DTO
 * Response DTO for paginated project type list
 */
public class ProjectTypeListResponse {
    private List<ProjectTypeResponseDTO> content;
    private PageInfo page;

    // Constructors
    public ProjectTypeListResponse() {
    }

    public ProjectTypeListResponse(List<ProjectTypeResponseDTO> content, PageInfo page) {
        this.content = content;
        this.page = page;
    }

    // Getters and Setters
    public List<ProjectTypeResponseDTO> getContent() {
        return content;
    }

    public void setContent(List<ProjectTypeResponseDTO> content) {
        this.content = content;
    }

    public PageInfo getPage() {
        return page;
    }

    public void setPage(PageInfo page) {
        this.page = page;
    }

    /**
     * Page Info nested class
     */
    public static class PageInfo {
        private Integer number;
        private Integer size;
        private Long totalElements;
        private Integer totalPages;

        // Constructors
        public PageInfo() {
        }

        public PageInfo(Integer number, Integer size, Long totalElements, Integer totalPages) {
            this.number = number;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }

        // Getters and Setters
        public Integer getNumber() {
            return number;
        }

        public void setNumber(Integer number) {
            this.number = number;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
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
    }
}

