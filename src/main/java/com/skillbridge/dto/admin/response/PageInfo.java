package com.skillbridge.dto.admin.response;

public class PageInfo {
    private long total;
    private int totalPages;
    private int currentPage;
    private int pageSize;

    // Constructors
    public PageInfo() {
    }

    public PageInfo(long total, int totalPages, int currentPage, int pageSize) {
        this.total = total;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    // Getters and Setters
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
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
