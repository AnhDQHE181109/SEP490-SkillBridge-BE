package com.skillbridge.dto.contract.response;

import java.util.List;

/**
 * Response DTO for contract list
 */
public class ContractListResponse {
    private List<ContractListItemDTO> contracts;
    private int currentPage;
    private int totalPages;
    private long totalElements;

    // Getters and Setters
    public List<ContractListItemDTO> getContracts() {
        return contracts;
    }

    public void setContracts(List<ContractListItemDTO> contracts) {
        this.contracts = contracts;
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

