package com.skillbridge.entity.contract;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SOW Contract Entity
 * Represents a SOW (Statement of Work) contract for a client
 * SOW contracts are separate from MSA contracts
 */
@Entity
@Table(name = "sow_contracts")
public class SOWContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "client_id", nullable = false)
    private Integer clientId;

    @Column(name = "contract_name", nullable = false, length = 255)
    private String contractName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(50)")
    private SOWContractStatus status = SOWContractStatus.Draft;

    @Column(name = "engagement_type", nullable = false, length = 50)
    private String engagementType; // "Fixed Price" or "Retainer"

    @Column(name = "parent_msa_id", nullable = false)
    private Integer parentMsaId; // Reference to parent MSA contract

    @Column(name = "project_name", nullable = false, length = 255)
    private String projectName;

    @Column(name = "scope_summary", columnDefinition = "TEXT")
    private String scopeSummary;

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
    public SOWContract() {
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

    public SOWContractStatus getStatus() {
        return status;
    }

    public void setStatus(SOWContractStatus status) {
        this.status = status;
    }

    public String getEngagementType() {
        return engagementType;
    }

    public void setEngagementType(String engagementType) {
        this.engagementType = engagementType;
    }

    public Integer getParentMsaId() {
        return parentMsaId;
    }

    public void setParentMsaId(Integer parentMsaId) {
        this.parentMsaId = parentMsaId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getScopeSummary() {
        return scopeSummary;
    }

    public void setScopeSummary(String scopeSummary) {
        this.scopeSummary = scopeSummary;
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
    public enum SOWContractStatus {
        Draft, Active, Pending, Under_Review, Request_for_Change, Approved, Completed, Terminated, Cancelled
    }
}

