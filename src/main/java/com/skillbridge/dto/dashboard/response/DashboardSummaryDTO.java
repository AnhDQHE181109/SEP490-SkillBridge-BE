package com.skillbridge.dto.dashboard.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Dashboard Summary DTO
 * Contains summary statistics for Contacts, Proposals, Contracts, and Change Requests
 */
public class DashboardSummaryDTO {

    @JsonProperty("contacts")
    private ContactsSummary contacts;

    @JsonProperty("proposals")
    private ProposalsSummary proposals;

    @JsonProperty("contracts")
    private ContractsSummary contracts;

    @JsonProperty("changeRequests")
    private ChangeRequestsSummary changeRequests;

    // Constructors
    public DashboardSummaryDTO() {
        this.contacts = new ContactsSummary();
        this.proposals = new ProposalsSummary();
        this.contracts = new ContractsSummary();
        this.changeRequests = new ChangeRequestsSummary();
    }

    // Getters and Setters
    public ContactsSummary getContacts() {
        return contacts;
    }

    public void setContacts(ContactsSummary contacts) {
        this.contacts = contacts;
    }

    public ProposalsSummary getProposals() {
        return proposals;
    }

    public void setProposals(ProposalsSummary proposals) {
        this.proposals = proposals;
    }

    public ContractsSummary getContracts() {
        return contracts;
    }

    public void setContracts(ContractsSummary contracts) {
        this.contracts = contracts;
    }

    public ChangeRequestsSummary getChangeRequests() {
        return changeRequests;
    }

    public void setChangeRequests(ChangeRequestsSummary changeRequests) {
        this.changeRequests = changeRequests;
    }

    // Inner classes for summary data
    public static class ContactsSummary {
        @JsonProperty("inprogress")
        private Integer inprogress = 0;

        @JsonProperty("new")
        private Integer newCount = 0;

        public Integer getInprogress() {
            return inprogress;
        }

        public void setInprogress(Integer inprogress) {
            this.inprogress = inprogress;
        }

        public Integer getNewCount() {
            return newCount;
        }

        public void setNewCount(Integer newCount) {
            this.newCount = newCount;
        }
    }

    public static class ProposalsSummary {
        @JsonProperty("underReview")
        private Integer underReview = 0;

        @JsonProperty("reviewed")
        private Integer reviewed = 0;

        public Integer getUnderReview() {
            return underReview;
        }

        public void setUnderReview(Integer underReview) {
            this.underReview = underReview;
        }

        public Integer getReviewed() {
            return reviewed;
        }

        public void setReviewed(Integer reviewed) {
            this.reviewed = reviewed;
        }
    }

    public static class ContractsSummary {
        @JsonProperty("active")
        private Integer active = 0;

        @JsonProperty("draft")
        private Integer draft = 0;

        public Integer getActive() {
            return active;
        }

        public void setActive(Integer active) {
            this.active = active;
        }

        public Integer getDraft() {
            return draft;
        }

        public void setDraft(Integer draft) {
            this.draft = draft;
        }
    }

    public static class ChangeRequestsSummary {
        @JsonProperty("underReview")
        private Integer underReview = 0;

        @JsonProperty("approved")
        private Integer approved = 0;

        public Integer getUnderReview() {
            return underReview;
        }

        public void setUnderReview(Integer underReview) {
            this.underReview = underReview;
        }

        public Integer getApproved() {
            return approved;
        }

        public void setApproved(Integer approved) {
            this.approved = approved;
        }
    }
}

