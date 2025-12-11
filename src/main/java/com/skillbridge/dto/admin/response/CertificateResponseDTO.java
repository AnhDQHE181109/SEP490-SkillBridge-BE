package com.skillbridge.dto.admin.response;

import java.time.LocalDate;

/**
 * Certificate Response DTO
 * Response DTO for certificate information
 */
public class CertificateResponseDTO {
    private Integer id;
    private Integer engineerId;
    private String name;
    private String issuedBy;
    private LocalDate issuedDate;
    private LocalDate expiryDate;

    // Constructors
    public CertificateResponseDTO() {
    }

    public CertificateResponseDTO(Integer id, Integer engineerId, String name, String issuedBy, 
                                  LocalDate issuedDate, LocalDate expiryDate) {
        this.id = id;
        this.engineerId = engineerId;
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

    public Integer getEngineerId() {
        return engineerId;
    }

    public void setEngineerId(Integer engineerId) {
        this.engineerId = engineerId;
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
}

