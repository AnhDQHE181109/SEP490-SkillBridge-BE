package com.skillbridge.dto.contract.response;

/**
 * Delivery Item DTO
 * Data transfer object for delivery item information (Retainer SOW)
 */
public class DeliveryItemDTO {
    private Integer id;
    private String milestone; // "November 2025", "December 2025", etc.
    private String deliveryNote; // "2 Middle Backend(100%)", etc.
    private String amount; // Currency format
    private String paymentDate; // Format: YYYY/MM/DD

    // Constructors
    public DeliveryItemDTO() {
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

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }
}

