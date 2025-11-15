package com.skillbridge.dto.contract.response;

/**
 * Fixed Price Billing Detail DTO
 * Data transfer object for billing detail information (Fixed Price SOW)
 */
public class FixedPriceBillingDetailDTO {
    private Integer id;
    private String billingName; // "Kickoff Payment", "Design Payment", etc.
    private String milestone; // Associated milestone
    private String amount; // Currency format
    private String percentage; // "20%", "30%", etc. or null for change requests
    private String invoiceDate; // Format: YYYY/MM/DD

    // Constructors
    public FixedPriceBillingDetailDTO() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBillingName() {
        return billingName;
    }

    public void setBillingName(String billingName) {
        this.billingName = billingName;
    }

    public String getMilestone() {
        return milestone;
    }

    public void setMilestone(String milestone) {
        this.milestone = milestone;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getPercentage() {
        return percentage;
    }

    public void setPercentage(String percentage) {
        this.percentage = percentage;
    }

    public String getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(String invoiceDate) {
        this.invoiceDate = invoiceDate;
    }
}

