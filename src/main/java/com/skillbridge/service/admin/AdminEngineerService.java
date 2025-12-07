package com.skillbridge.service.admin;

import com.skillbridge.dto.admin.request.CertificateRequest;
import com.skillbridge.dto.admin.request.CreateEngineerRequest;
import com.skillbridge.dto.admin.request.UpdateEngineerRequest;
import com.skillbridge.dto.admin.response.EngineerListResponse;
import com.skillbridge.dto.admin.response.EngineerResponseDTO;
import com.skillbridge.entity.engineer.Certificate;
import com.skillbridge.entity.engineer.Engineer;
import com.skillbridge.entity.engineer.EngineerSkill;
import com.skillbridge.repository.contract.ChangeRequestEngagedEngineerRepository;
import com.skillbridge.repository.contract.SOWEngagedEngineerBaseRepository;
import com.skillbridge.repository.contract.SOWEngagedEngineerRepository;
import com.skillbridge.repository.engineer.CertificateRepository;
import com.skillbridge.repository.engineer.EngineerRepository;
import com.skillbridge.repository.engineer.EngineerSkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Engineer Service
 * Handles all engineer operations for Admin Engineer List
 */
@Service
@Transactional
public class AdminEngineerService {

    @Autowired
    private EngineerRepository engineerRepository;

    @Autowired
    private SOWEngagedEngineerRepository sowEngagedEngineerRepository;

    @Autowired
    private SOWEngagedEngineerBaseRepository sowEngagedEngineerBaseRepository;

    @Autowired
    private ChangeRequestEngagedEngineerRepository changeRequestEngagedEngineerRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private EngineerSkillRepository engineerSkillRepository;

    /**
     * Get all engineers with pagination and search
     */
    public EngineerListResponse getAllEngineers(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Engineer> engineerPage;

        if (search != null && !search.trim().isEmpty()) {
            String searchTerm = search.trim();
            engineerPage = engineerRepository.findByFullNameContainingIgnoreCaseOrPrimarySkillContainingIgnoreCase(
                    searchTerm, searchTerm, pageable);
        } else {
            engineerPage = engineerRepository.findAll(pageable);
        }

        List<EngineerResponseDTO> content = engineerPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        EngineerListResponse.PageInfo pageInfo = new EngineerListResponse.PageInfo(
                engineerPage.getTotalElements(),
                engineerPage.getTotalPages(),
                engineerPage.getNumber(),
                engineerPage.getSize()
        );

        EngineerListResponse response = new EngineerListResponse();
        response.setContent(content);
        response.setPage(pageInfo);

        return response;
    }

    /**
     * Get engineer by ID
     */
    public EngineerResponseDTO getEngineerById(Integer engineerId) {
        Engineer engineer = engineerRepository.findById(engineerId)
                .orElseThrow(() -> new RuntimeException("Engineer not found"));
        return convertToDTO(engineer);
    }

    /**
     * Get engineer detail by ID with certificates and skills
     */
    public com.skillbridge.dto.admin.response.EngineerDetailResponseDTO getEngineerDetailById(Integer engineerId) {
        Engineer engineer = engineerRepository.findById(engineerId)
                .orElseThrow(() -> new RuntimeException("Engineer not found"));
        
        com.skillbridge.dto.admin.response.EngineerDetailResponseDTO detailDTO = new com.skillbridge.dto.admin.response.EngineerDetailResponseDTO();
        EngineerResponseDTO baseDTO = convertToDTO(engineer);
        
        // Copy base fields
        detailDTO.setId(baseDTO.getId());
        detailDTO.setFullName(baseDTO.getFullName());
        detailDTO.setEmail(baseDTO.getEmail());
        detailDTO.setPhone(baseDTO.getPhone());
        detailDTO.setGender(baseDTO.getGender());
        detailDTO.setDateOfBirth(baseDTO.getDateOfBirth());
        detailDTO.setYearsExperience(baseDTO.getYearsExperience());
        detailDTO.setSeniority(baseDTO.getSeniority());
        detailDTO.setPrimarySkill(baseDTO.getPrimarySkill());
        detailDTO.setSalaryExpectation(baseDTO.getSalaryExpectation());
        detailDTO.setLocation(baseDTO.getLocation());
        detailDTO.setStatus(baseDTO.getStatus());
        detailDTO.setSummary(baseDTO.getSummary());
        detailDTO.setIntroduction(baseDTO.getIntroduction());
        detailDTO.setLanguageSummary(baseDTO.getLanguageSummary());
        detailDTO.setProfileImageUrl(baseDTO.getProfileImageUrl());
        detailDTO.setInterestedInJapan(baseDTO.getInterestedInJapan());
        detailDTO.setProjectTypeExperience(baseDTO.getProjectTypeExperience());
        detailDTO.setCreatedAt(baseDTO.getCreatedAt());
        detailDTO.setUpdatedAt(baseDTO.getUpdatedAt());
        
        // Load certificates
        List<Certificate> certificates = certificateRepository.findByEngineerId(engineerId);
        List<com.skillbridge.dto.admin.response.CertificateResponseDTO> certificateDTOs = certificates.stream()
                .map(cert -> {
                    com.skillbridge.dto.admin.response.CertificateResponseDTO certDTO = new com.skillbridge.dto.admin.response.CertificateResponseDTO();
                    certDTO.setId(cert.getId());
                    certDTO.setEngineerId(cert.getEngineerId());
                    certDTO.setName(cert.getName());
                    certDTO.setIssuedBy(cert.getIssuedBy());
                    certDTO.setIssuedDate(cert.getIssuedDate());
                    certDTO.setExpiryDate(cert.getExpiryDate());
                    return certDTO;
                })
                .collect(java.util.stream.Collectors.toList());
        detailDTO.setCertificates(certificateDTOs);
        
        // Load engineer skills
        List<EngineerSkill> engineerSkills = engineerSkillRepository.findByEngineerId(engineerId);
        List<Integer> skillIds = engineerSkills.stream()
                .map(EngineerSkill::getSkillId)
                .collect(java.util.stream.Collectors.toList());
        detailDTO.setOtherSkills(skillIds);
        
        return detailDTO;
    }

    /**
     * Create a new engineer with all details including certificates and skills
     */
    public EngineerResponseDTO createEngineer(CreateEngineerRequest request) {
        // Check email uniqueness
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            engineerRepository.findByEmail(request.getEmail())
                    .ifPresent(e -> {
                        throw new RuntimeException("Email already exists. Please use a different email.");
                    });
        }

        // Create engineer entity
        Engineer engineer = new Engineer();
        engineer.setFullName(request.getFullName());
        engineer.setEmail(request.getEmail());
        engineer.setPhone(request.getPhone());
        engineer.setGender(request.getGender());
        engineer.setDateOfBirth(request.getDateOfBirth());
        engineer.setYearsExperience(request.getYearsExperience());
        engineer.setSeniority(request.getSeniority());
        engineer.setPrimarySkill(request.getPrimarySkill());
        engineer.setSalaryExpectation(request.getSalaryExpectation());
        engineer.setLocation(request.getLocation());
        engineer.setStatus(request.getStatus());
        engineer.setSummary(request.getSummary());
        engineer.setIntroduction(request.getIntroduction());
        engineer.setLanguageSummary(request.getLanguageSummary());
        engineer.setProfileImageUrl(request.getProfileImageUrl());
        engineer.setInterestedInJapan(request.getInterestedInJapan());
        engineer.setProjectTypeExperience(request.getProjectTypeExperience());

        engineer = engineerRepository.save(engineer);

        // Create certificates if provided
        if (request.getCertificates() != null && !request.getCertificates().isEmpty()) {
            for (CertificateRequest certRequest : request.getCertificates()) {
                if (certRequest.getName() != null && !certRequest.getName().trim().isEmpty()) {
                    Certificate certificate = new Certificate();
                    certificate.setEngineerId(engineer.getId());
                    certificate.setName(certRequest.getName());
                    certificate.setIssuedBy(certRequest.getIssuedBy());
                    certificate.setIssuedDate(certRequest.getIssuedDate());
                    certificate.setExpiryDate(certRequest.getExpiryDate());
                    certificateRepository.save(certificate);
                }
            }
        }

        // Create engineer_skills if Other Skills provided
        if (request.getOtherSkills() != null && !request.getOtherSkills().isEmpty()) {
            for (Integer skillId : request.getOtherSkills()) {
                EngineerSkill engineerSkill = new EngineerSkill();
                engineerSkill.setEngineerId(engineer.getId());
                engineerSkill.setSkillId(skillId);
                // Set default level and years if not specified (can be enhanced later)
                engineerSkill.setLevel("Intermediate");
                engineerSkill.setYears(0);
                engineerSkillRepository.save(engineerSkill);
            }
        }

        return convertToDTO(engineer);
    }

    /**
     * Update an engineer with all details including certificates and skills
     */
    public EngineerResponseDTO updateEngineer(Integer engineerId, UpdateEngineerRequest request) {
        Engineer engineer = engineerRepository.findById(engineerId)
                .orElseThrow(() -> new RuntimeException("Engineer not found"));

        // Check email uniqueness (excluding current engineer)
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            engineerRepository.findByEmail(request.getEmail())
                    .ifPresent(e -> {
                        if (!e.getId().equals(engineerId)) {
                            throw new RuntimeException("Email already exists. Please use a different email.");
                        }
                    });
        }

        // Update engineer entity
        engineer.setFullName(request.getFullName());
        engineer.setEmail(request.getEmail());
        engineer.setPhone(request.getPhone());
        engineer.setGender(request.getGender());
        engineer.setDateOfBirth(request.getDateOfBirth());
        engineer.setYearsExperience(request.getYearsExperience());
        engineer.setSeniority(request.getSeniority());
        engineer.setPrimarySkill(request.getPrimarySkill());
        engineer.setSalaryExpectation(request.getSalaryExpectation());
        engineer.setLocation(request.getLocation());
        engineer.setStatus(request.getStatus());
        engineer.setSummary(request.getSummary());
        engineer.setIntroduction(request.getIntroduction());
        engineer.setLanguageSummary(request.getLanguageSummary());
        engineer.setProfileImageUrl(request.getProfileImageUrl());
        engineer.setInterestedInJapan(request.getInterestedInJapan());
        engineer.setProjectTypeExperience(request.getProjectTypeExperience());

        engineer = engineerRepository.save(engineer);

        // Delete all existing certificates for this engineer
        certificateRepository.findByEngineerId(engineerId).forEach(certificateRepository::delete);

        // Create new certificates if provided
        if (request.getCertificates() != null && !request.getCertificates().isEmpty()) {
            for (CertificateRequest certRequest : request.getCertificates()) {
                if (certRequest.getName() != null && !certRequest.getName().trim().isEmpty()) {
                    Certificate certificate = new Certificate();
                    certificate.setEngineerId(engineer.getId());
                    certificate.setName(certRequest.getName());
                    certificate.setIssuedBy(certRequest.getIssuedBy());
                    certificate.setIssuedDate(certRequest.getIssuedDate());
                    certificate.setExpiryDate(certRequest.getExpiryDate());
                    certificateRepository.save(certificate);
                }
            }
        }

        // Delete all existing engineer_skills for this engineer
        engineerSkillRepository.findByEngineerId(engineerId).forEach(engineerSkillRepository::delete);

        // Create new engineer_skills if Other Skills provided
        if (request.getOtherSkills() != null && !request.getOtherSkills().isEmpty()) {
            for (Integer skillId : request.getOtherSkills()) {
                EngineerSkill engineerSkill = new EngineerSkill();
                engineerSkill.setEngineerId(engineer.getId());
                engineerSkill.setSkillId(skillId);
                // Set default level and years if not specified (can be enhanced later)
                engineerSkill.setLevel("Intermediate");
                engineerSkill.setYears(0);
                engineerSkillRepository.save(engineerSkill);
            }
        }

        return convertToDTO(engineer);
    }

    /**
     * Delete an engineer
     * Validates that engineer is not associated with any contracts or engagements
     */
    public void deleteEngineer(Integer engineerId) {
        Engineer engineer = engineerRepository.findById(engineerId)
                .orElseThrow(() -> new RuntimeException("Engineer not found"));

        // Check if engineer is associated with any active engagements
        if (isEngineerAssociatedWithEngagements(engineerId)) {
            throw new RuntimeException("Cannot delete engineer. Engineer is associated with active contracts or engagements.");
        }

        // Delete engineer
        engineerRepository.deleteById(engineerId);
    }

    /**
     * Check if engineer is associated with any contracts or engagements
     */
    private boolean isEngineerAssociatedWithEngagements(Integer engineerId) {
        // Check SOW Engaged Engineer Base
        // Note: SOWEngagedEngineerBase has engineerId field but it's optional
        // We'll check if there are any base engineers with this ID
        List<com.skillbridge.entity.contract.SOWEngagedEngineerBase> baseEngineers = 
            sowEngagedEngineerBaseRepository.findAll();
        boolean hasBaseEngagement = baseEngineers.stream()
                .anyMatch(base -> base.getEngineerId() != null && base.getEngineerId().equals(engineerId));
        
        if (hasBaseEngagement) {
            return true;
        }

        // Note: SOWEngagedEngineer and ChangeRequestEngagedEngineer don't have engineer_id field
        // They only have engineer_level. For now, we'll only check base engagements.
        // In the future, when engineer_id is properly linked, we can add more checks.
        
        return false;
    }

    /**
     * Convert Engineer entity to EngineerResponseDTO
     */
    private EngineerResponseDTO convertToDTO(Engineer engineer) {
        EngineerResponseDTO dto = new EngineerResponseDTO();
        dto.setId(engineer.getId());
        dto.setFullName(engineer.getFullName());
        dto.setEmail(engineer.getEmail());
        dto.setPhone(engineer.getPhone());
        dto.setGender(engineer.getGender());
        dto.setDateOfBirth(engineer.getDateOfBirth());
        dto.setYearsExperience(engineer.getYearsExperience());
        dto.setSeniority(engineer.getSeniority());
        dto.setPrimarySkill(engineer.getPrimarySkill());
        dto.setSalaryExpectation(engineer.getSalaryExpectation());
        dto.setLocation(engineer.getLocation());
        dto.setStatus(engineer.getStatus());
        dto.setSummary(engineer.getSummary());
        dto.setIntroduction(engineer.getIntroduction());
        dto.setLanguageSummary(engineer.getLanguageSummary());
        dto.setProfileImageUrl(engineer.getProfileImageUrl());
        dto.setInterestedInJapan(engineer.getInterestedInJapan());
        dto.setProjectTypeExperience(engineer.getProjectTypeExperience());
        dto.setCreatedAt(engineer.getCreatedAt());
        dto.setUpdatedAt(engineer.getUpdatedAt());
        return dto;
    }
}

