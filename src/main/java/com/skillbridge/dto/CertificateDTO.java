package com.skillbridge.dto;

import java.time.LocalDate;

/**
 * Certificate DTO
 * Represents a professional certification
 */
public class CertificateDTO {
    private Integer id;
    private String name;
    private String issuedBy;
    private LocalDate issuedDate;
    private LocalDate expiryDate;

    // Constructors
    public CertificateDTO() {
    }

    public CertificateDTO(Integer id, String name, String issuedBy) {
        this.id = id;
        this.name = name;
        this.issuedBy = issuedBy;
    }

    public CertificateDTO(Integer id, String name, String issuedBy, LocalDate issuedDate, LocalDate expiryDate) {
        this.id = id;
        this.name = name;
        this.issuedBy = issuedBy;
        this.issuedDate = issuedDate;
        this.expiryDate = expiryDate;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    @Override
    public String toString() {
        return "CertificateDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", issuedBy='" + issuedBy + '\'' +
                ", issuedDate=" + issuedDate +
                ", expiryDate=" + expiryDate +
                '}';
    }
}

