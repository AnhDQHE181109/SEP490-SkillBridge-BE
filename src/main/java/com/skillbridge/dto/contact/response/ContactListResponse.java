package com.skillbridge.dto.contact.response;

import java.util.List;

/**
 * Contact List Response DTO
 * Response object for contact list API
 */
public class ContactListResponse {

    private List<ContactListItemDTO> contacts;
    private int currentPage;
    private int totalPages;
    private long totalElements;

    // Constructors
    public ContactListResponse() {
    }

    public ContactListResponse(List<ContactListItemDTO> contacts, int currentPage, int totalPages, long totalElements) {
        this.contacts = contacts;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }

    // Getters and Setters
    public List<ContactListItemDTO> getContacts() {
        return contacts;
    }

    public void setContacts(List<ContactListItemDTO> contacts) {
        this.contacts = contacts;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }
}

