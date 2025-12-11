package com.skillbridge.dto.admin.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Certificate Request DTO
 * Request body for creating/updating a certificate
 */
public class CertificateRequest {

    @Size(max = 255, message = "Certificate name must not exceed 255 characters")
    private String name;

    @Size(max = 255, message = "Issued by must not exceed 255 characters")
    private String issuedBy;

    private LocalDate issuedDate;

    private LocalDate expiryDate;

    // Constructors
    public CertificateRequest() {
    }

    public CertificateRequest(String name, String issuedBy, LocalDate issuedDate, LocalDate expiryDate) {
        this.name = name;
        this.issuedBy = issuedBy;
        this.issuedDate = issuedDate;
        this.expiryDate = expiryDate;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(LocalDate issuedDate) {
        this.issuedDate = issuedDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
}

