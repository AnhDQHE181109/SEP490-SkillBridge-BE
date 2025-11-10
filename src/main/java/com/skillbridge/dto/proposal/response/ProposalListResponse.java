package com.skillbridge.dto.proposal.response;

import java.util.List;

public class ProposalListResponse {

    private List<ProposalListItemDTO> proposalsList;
    private Long totalItems;
    private Integer currentPage;
    private Integer totalPages;

    public ProposalListResponse() {}

    public List<ProposalListItemDTO> getProposalsList() {
        return proposalsList;
    }

    public void setProposalsList(List<ProposalListItemDTO> proposalsList) {
        this.proposalsList = proposalsList;
    }

    public Long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Long totalItems) {
        this.totalItems = totalItems;
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
}
