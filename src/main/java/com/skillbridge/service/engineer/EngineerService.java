package com.skillbridge.service.engineer;

import com.skillbridge.dto.engineer.response.CertificateDTO;
import com.skillbridge.dto.engineer.response.EngineerDetailDTO;
import com.skillbridge.dto.engineer.response.SkillDTO;
import com.skillbridge.entity.engineer.Certificate;
import com.skillbridge.entity.engineer.Engineer;
import com.skillbridge.repository.engineer.CertificateRepository;
import com.skillbridge.repository.engineer.EngineerRepository;
import com.skillbridge.repository.engineer.EngineerSkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Engineer Service
 * Handles business logic for engineer operations
 */
@Service
public class EngineerService {

    @Autowired
    private EngineerRepository engineerRepository;

    @Autowired
    private EngineerSkillRepository engineerSkillRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    /**
     * Get detailed engineer information by ID
     * @param id Engineer ID
     * @return EngineerDetailDTO with complete profile information
     */
    public EngineerDetailDTO getEngineerDetailById(Integer id) {
        // Find engineer
        Engineer engineer = engineerRepository.findById(id)
            .orElse(null);
            
        if (engineer == null) {
            return null;
        }
        
        // Create DTO and map basic fields
        EngineerDetailDTO dto = new EngineerDetailDTO();
        dto.setId(engineer.getId());
        dto.setFullName(engineer.getFullName());
        dto.setLocation(engineer.getLocation());
        dto.setProfileImageUrl(engineer.getProfileImageUrl());
        dto.setSalaryExpectation(engineer.getSalaryExpectation());
        dto.setYearsExperience(engineer.getYearsExperience());
        dto.setSeniority(engineer.getSeniority());
        dto.setStatus(engineer.getStatus());
        dto.setPrimarySkill(engineer.getPrimarySkill());
        dto.setLanguageSummary(engineer.getLanguageSummary());
        dto.setSummary(engineer.getSummary());
        dto.setIntroduction(engineer.getIntroduction());
        
        // Load skills (using custom query with join)
        List<SkillDTO> skills = engineerSkillRepository.findSkillsByEngineerId(id);
        dto.setSkills(skills);
        
        // Load certificates
        List<Certificate> certificates = certificateRepository.findByEngineerId(id);
        List<CertificateDTO> certificateDTOs = certificates.stream()
            .map(this::convertToCertificateDTO)
            .collect(Collectors.toList());
        dto.setCertificates(certificateDTOs);
        
        return dto;
    }

    /**
     * Convert Certificate entity to CertificateDTO
     */
    private CertificateDTO convertToCertificateDTO(Certificate certificate) {
        CertificateDTO dto = new CertificateDTO();
        dto.setId(certificate.getId());
        dto.setName(certificate.getName());
        dto.setIssuedBy(certificate.getIssuedBy());
        dto.setIssuedDate(certificate.getIssuedDate());
        dto.setExpiryDate(certificate.getExpiryDate());
        return dto;
    }
}

