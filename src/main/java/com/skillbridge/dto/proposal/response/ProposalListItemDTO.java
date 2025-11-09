package com.skillbridge.dto.proposal.response;

/**
 * Proposal List Item DTO
 * Represents an item on the list on the list of proposals
 */
public class ProposalListItemDTO {

    private Integer no;
    private Integer internalProposalID; //Internal ID used for identifying records
    private String proposalID;  //Displayed on the list as: P-YYYY-NN
    private String title;
    private String contactID;   //Displayed on the list as: CT-YYYY-NN
    private Integer contactInternalID;  //Internal contact ID used for identifying records
    private String contactDescription;
    private String createdOn;   //Format: YYYY/MM/DD
    private String status;  //"Under review",  "Request for change", "Approved"
    private String lastUpdated; // Format: YYYY/MM/DD

    public ProposalListItemDTO() {}

    public Integer getNo() {
        return no;
    }

    public void setNo(Integer no) {
        this.no = no;
    }

    public Integer getInternalProposalID() {
        return internalProposalID;
    }

    public void setInternalProposalID(Integer internalProposalID) {
        this.internalProposalID = internalProposalID;
    }

    public String getProposalID() {
        return proposalID;
    }

    public void setProposalID(String proposalID) {
        this.proposalID = proposalID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContactID() {
        return contactID;
    }

    public void setContactID(String contactID) {
        this.contactID = contactID;
    }

    public Integer getContactInternalID() {
        return contactInternalID;
    }

    public void setContactInternalID(Integer contactInternalID) {
        this.contactInternalID = contactInternalID;
    }

    public String getContactDescription() {
        return contactDescription;
    }

    public void setContactDescription(String contactDescription) {
        this.contactDescription = contactDescription;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
