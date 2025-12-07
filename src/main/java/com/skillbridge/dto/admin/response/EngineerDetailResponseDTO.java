package com.skillbridge.dto.admin.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Engineer Detail Response DTO
 * Response DTO for engineer detail with certificates and skills
 */
public class EngineerDetailResponseDTO extends EngineerResponseDTO {
    private List<CertificateResponseDTO> certificates;
    private List<Integer> otherSkills; // Array of skill IDs

    // Constructors
    public EngineerDetailResponseDTO() {
        super();
    }

    // Getters and Setters
    public List<CertificateResponseDTO> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<CertificateResponseDTO> certificates) {
        this.certificates = certificates;
    }

    public List<Integer> getOtherSkills() {
        return otherSkills;
    }

    public void setOtherSkills(List<Integer> otherSkills) {
        this.otherSkills = otherSkills;
    }
}

