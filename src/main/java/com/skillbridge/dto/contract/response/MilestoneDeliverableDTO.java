package com.skillbridge.dto.contract.response;

/**
 * Milestone Deliverable DTO
 * Data transfer object for milestone deliverable information (Fixed Price SOW)
 */
public class MilestoneDeliverableDTO {
    private Integer id;
    private String milestone; // "Kickoff", "Design Phase", etc.
    private String deliveryNote;
    private String acceptanceCriteria;
    private String plannedEnd; // Format: YYYY/MM/DD
    private String paymentPercentage; // "20%", "30%", etc.

    // Constructors
    public MilestoneDeliverableDTO() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMilestone() {
        return milestone;
    }

    public void setMilestone(String milestone) {
        this.milestone = milestone;
    }

    public String getDeliveryNote() {
        return deliveryNote;
    }

    public void setDeliveryNote(String deliveryNote) {
        this.deliveryNote = deliveryNote;
    }

    public String getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    public void setAcceptanceCriteria(String acceptanceCriteria) {
        this.acceptanceCriteria = acceptanceCriteria;
    }

    public String getPlannedEnd() {
        return plannedEnd;
    }

    public void setPlannedEnd(String plannedEnd) {
        this.plannedEnd = plannedEnd;
    }

    public String getPaymentPercentage() {
        return paymentPercentage;
    }

    public void setPaymentPercentage(String paymentPercentage) {
        this.paymentPercentage = paymentPercentage;
    }
}

