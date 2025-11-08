package com.skillbridge.entity.contract;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Contract Entity
 * Represents an MSA (Master Service Agreement) contract for a client
 * Note: SOW contracts are stored in SOWContract entity
 */
@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "client_id", nullable = false)
    private Integer clientId;

    @Column(name = "contract_name", nullable = false, length = 255)
    private String contractName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(50)")
    private ContractStatus status = ContractStatus.Draft;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "value", precision = 16, scale = 2)
    private BigDecimal value;

    @Column(name = "assignee_id", length = 50)
    private String assigneeId;

    // Commercial Terms
    @Column(name = "currency", length = 16)
    private String currency;

    @Column(name = "payment_terms", length = 128)
    private String paymentTerms;

    @Column(name = "invoicing_cycle", length = 64)
    private String invoicingCycle;

    @Column(name = "billing_day", length = 64)
    private String billingDay;

    @Column(name = "tax_withholding", length = 16)
    private String taxWithholding;

    // Legal / Compliance
    @Column(name = "ip_ownership", length = 128)
    private String ipOwnership;

    @Column(name = "governing_law", length = 64)
    private String governingLaw;

    // LandBridge Contact
    @Column(name = "landbridge_contact_name", length = 255)
    private String landbridgeContactName;

    @Column(name = "landbridge_contact_email", length = 255)
    private String landbridgeContactEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Contract() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public String getInvoicingCycle() {
        return invoicingCycle;
    }

    public void setInvoicingCycle(String invoicingCycle) {
        this.invoicingCycle = invoicingCycle;
    }

    public String getBillingDay() {
        return billingDay;
    }

    public void setBillingDay(String billingDay) {
        this.billingDay = billingDay;
    }

    public String getTaxWithholding() {
        return taxWithholding;
    }

    public void setTaxWithholding(String taxWithholding) {
        this.taxWithholding = taxWithholding;
    }

    public String getIpOwnership() {
        return ipOwnership;
    }

    public void setIpOwnership(String ipOwnership) {
        this.ipOwnership = ipOwnership;
    }

    public String getGoverningLaw() {
        return governingLaw;
    }

    public void setGoverningLaw(String governingLaw) {
        this.governingLaw = governingLaw;
    }

    public String getLandbridgeContactName() {
        return landbridgeContactName;
    }

    public void setLandbridgeContactName(String landbridgeContactName) {
        this.landbridgeContactName = landbridgeContactName;
    }

    public String getLandbridgeContactEmail() {
        return landbridgeContactEmail;
    }

    public void setLandbridgeContactEmail(String landbridgeContactEmail) {
        this.landbridgeContactEmail = landbridgeContactEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Enums
    public enum ContractStatus {
        Draft, Active, Pending, Under_Review, Request_for_Change, Approved, Completed, Terminated, Cancelled
    }
}

