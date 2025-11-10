package com.skillbridge.dto.proposal.response;

import java.util.List;

/**
 * Proposal List Response DTO
 * Contains paginated proposal list
 */
public class ProposalListResponse {
    private List<ProposalListItemDTO> proposals;
    private int currentPage;
    private int totalPages;
    private long totalElements;

    // Constructors
    public ProposalListResponse() {
    }

    // Getters and Setters
    public List<ProposalListItemDTO> getProposals() {
        return proposals;
    }

    public void setProposals(List<ProposalListItemDTO> proposals) {
        this.proposals = proposals;
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

