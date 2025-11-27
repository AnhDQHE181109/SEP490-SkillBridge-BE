package com.skillbridge.dto.sales.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Sales Dashboard Summary DTO
 * Contains summary statistics for Contacts, Opportunities, Proposals, Contracts, Change Requests, and Revenue
 */
public class SalesDashboardSummaryDTO {

    @JsonProperty("contacts")
    private ContactsSummary contacts;

    @JsonProperty("opportunities")
    private OpportunitiesSummary opportunities;

    @JsonProperty("proposals")
    private ProposalsSummary proposals;

    @JsonProperty("contracts")
    private ContractsSummary contracts;

    @JsonProperty("changeRequests")
    private ChangeRequestsSummary changeRequests;

    @JsonProperty("revenue")
    private List<RevenueItem> revenue; // Only for Sales Manager

    // Constructors
    public SalesDashboardSummaryDTO() {
        this.contacts = new ContactsSummary();
        this.opportunities = new OpportunitiesSummary();
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

    public OpportunitiesSummary getOpportunities() {
        return opportunities;
    }

    public void setOpportunities(OpportunitiesSummary opportunities) {
        this.opportunities = opportunities;
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

    public List<RevenueItem> getRevenue() {
        return revenue;
    }

    public void setRevenue(List<RevenueItem> revenue) {
        this.revenue = revenue;
    }

    // Inner classes for summary data
    public static class ContactsSummary {
        @JsonProperty("all")
        private Integer all = 0;

        @JsonProperty("new")
        private Integer newCount = 0;

        public Integer getAll() {
            return all;
        }

        public void setAll(Integer all) {
            this.all = all;
        }

        public Integer getNewCount() {
            return newCount;
        }

        public void setNewCount(Integer newCount) {
            this.newCount = newCount;
        }
    }

    public static class OpportunitiesSummary {
        @JsonProperty("all")
        private Integer all = 0;

        @JsonProperty("underReview")
        private Integer underReview = 0;

        public Integer getAll() {
            return all;
        }

        public void setAll(Integer all) {
            this.all = all;
        }

        public Integer getUnderReview() {
            return underReview;
        }

        public void setUnderReview(Integer underReview) {
            this.underReview = underReview;
        }
    }

    public static class ProposalsSummary {
        @JsonProperty("all")
        private Integer all = 0;

        @JsonProperty("underReview")
        private Integer underReview = 0;

        public Integer getAll() {
            return all;
        }

        public void setAll(Integer all) {
            this.all = all;
        }

        public Integer getUnderReview() {
            return underReview;
        }

        public void setUnderReview(Integer underReview) {
            this.underReview = underReview;
        }
    }

    public static class ContractsSummary {
        @JsonProperty("all")
        private Integer all = 0;

        @JsonProperty("underReview")
        private Integer underReview = 0;

        public Integer getAll() {
            return all;
        }

        public void setAll(Integer all) {
            this.all = all;
        }

        public Integer getUnderReview() {
            return underReview;
        }

        public void setUnderReview(Integer underReview) {
            this.underReview = underReview;
        }
    }

    public static class ChangeRequestsSummary {
        @JsonProperty("all")
        private Integer all = 0;

        @JsonProperty("underReview")
        private Integer underReview = 0;

        public Integer getAll() {
            return all;
        }

        public void setAll(Integer all) {
            this.all = all;
        }

        public Integer getUnderReview() {
            return underReview;
        }

        public void setUnderReview(Integer underReview) {
            this.underReview = underReview;
        }
    }

    public static class RevenueItem {
        @JsonProperty("month")
        private String month; // Format: "YYYY/MM"

        @JsonProperty("amount")
        private Long amount; // In JPY

        public RevenueItem() {
        }

        public RevenueItem(String month, Long amount) {
            this.month = month;
            this.amount = amount;
        }

        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public Long getAmount() {
            return amount;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }
    }
}

