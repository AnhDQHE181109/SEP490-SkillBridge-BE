package com.skillbridge.dto.contract.response;

/**
 * Retainer Billing Detail DTO
 * Data transfer object for billing detail information (Retainer SOW)
 */
public class RetainerBillingDetailDTO {
    private Integer id;
    private String paymentDate; // Format: YYYY/MM/DD
    private String deliveryNote; // "2 Middle Backend(100%)", etc.
    private String amount; // Currency format

    // Constructors
    public RetainerBillingDetailDTO() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
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
}

