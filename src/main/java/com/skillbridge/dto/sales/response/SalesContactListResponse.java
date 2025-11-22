package com.skillbridge.dto.sales.response;

import java.util.List;

/**
 * Sales Contact List Response DTO
 * Response object for sales contact list API
 */
public class SalesContactListResponse {

    private List<SalesContactListItemDTO> contacts;
    private int page;
    private int pageSize;
    private int totalPages;
    private long total;

    // Constructors
    public SalesContactListResponse() {
    }

    public SalesContactListResponse(List<SalesContactListItemDTO> contacts, int page, int pageSize, int totalPages, long total) {
        this.contacts = contacts;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.total = total;
    }

    // Getters and Setters
    public List<SalesContactListItemDTO> getContacts() {
        return contacts;
    }

    public void setContacts(List<SalesContactListItemDTO> contacts) {
        this.contacts = contacts;
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

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}

