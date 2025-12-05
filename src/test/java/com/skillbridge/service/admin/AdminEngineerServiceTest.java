package com.skillbridge.service.admin;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AdminEngineerService}
 */
@ExtendWith(MockitoExtension.class)
class AdminEngineerServiceTest {

    @Mock
    private EngineerRepository engineerRepository;

    @Mock
    private SOWEngagedEngineerRepository sowEngagedEngineerRepository;

    @Mock
    private SOWEngagedEngineerBaseRepository sowEngagedEngineerBaseRepository;

    @Mock
    private ChangeRequestEngagedEngineerRepository changeRequestEngagedEngineerRepository;

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private EngineerSkillRepository engineerSkillRepository;

    @InjectMocks
    private AdminEngineerService adminEngineerService;

    private Engineer sampleEngineer;

    @BeforeEach
    void setUp() {
        sampleEngineer = createEngineer(1, "John Doe", "john@example.com");
    }

    @Test
    @DisplayName("getAllEngineers - without search uses findAll and maps page info")
    void testGetAllEngineers_NoSearch() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Engineer> page = new PageImpl<>(List.of(sampleEngineer), pageable, 1);
        when(engineerRepository.findAll(pageable)).thenReturn(page);

        // Act
        EngineerListResponse response = adminEngineerService.getAllEngineers(0, 10, null);

        // Assert
        assertEquals(1, response.getContent().size());
        assertEquals("John Doe", response.getContent().get(0).getFullName());
        assertEquals(1, response.getPage().getTotalElements());
        assertEquals(1, response.getPage().getTotalPages());
        verify(engineerRepository).findAll(pageable);
        verify(engineerRepository, never())
                .findByFullNameContainingIgnoreCaseOrPrimarySkillContainingIgnoreCase(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("getAllEngineers - with search term uses search repository")
    void testGetAllEngineers_WithSearch() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 5);
        Page<Engineer> page = new PageImpl<>(List.of(sampleEngineer), pageable, 1);
        when(engineerRepository.findByFullNameContainingIgnoreCaseOrPrimarySkillContainingIgnoreCase("john", "john", pageable))
                .thenReturn(page);

        // Act
        EngineerListResponse response = adminEngineerService.getAllEngineers(0, 5, " john ");

        // Assert
        assertEquals(1, response.getContent().size());
        verify(engineerRepository)
                .findByFullNameContainingIgnoreCaseOrPrimarySkillContainingIgnoreCase("john", "john", pageable);
    }

    @Test
    @DisplayName("getEngineerById - throws when engineer not found")
    void testGetEngineerById_NotFound() {
        when(engineerRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> adminEngineerService.getEngineerById(99));
    }

    @Test
    @DisplayName("getEngineerDetailById - returns certificates and other skills")
    void testGetEngineerDetailById_Success() {
        // Arrange
        when(engineerRepository.findById(1)).thenReturn(Optional.of(sampleEngineer));

        Certificate cert = new Certificate();
        cert.setId(10);
        cert.setEngineerId(1);
        cert.setName("AWS");
        when(certificateRepository.findByEngineerId(1)).thenReturn(List.of(cert));

        EngineerSkill engineerSkill = new EngineerSkill();
        engineerSkill.setEngineerId(1);
        engineerSkill.setSkillId(100);
        when(engineerSkillRepository.findByEngineerId(1)).thenReturn(List.of(engineerSkill));

        // Act
        var detail = adminEngineerService.getEngineerDetailById(1);

        // Assert
        assertEquals(1, detail.getCertificates().size());
        assertEquals("AWS", detail.getCertificates().get(0).getName());
        assertEquals(List.of(100), detail.getOtherSkills());
    }

    @Test
    @DisplayName("createEngineer - throws when email already exists")
    void testCreateEngineer_EmailExists() {
        CreateEngineerRequest request = new CreateEngineerRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");

        when(engineerRepository.findByEmail("john@example.com")).thenReturn(Optional.of(new Engineer()));

        assertThrows(RuntimeException.class, () -> adminEngineerService.createEngineer(request));
        verify(engineerRepository, never()).save(any(Engineer.class));
    }

    @Test
    @DisplayName("createEngineer - saves engineer, certificates, and skills")
    void testCreateEngineer_Success() {
        CreateEngineerRequest request = new CreateEngineerRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setStatus("AVAILABLE");
        request.setPrimarySkill("Java");
        request.setSalaryExpectation(BigDecimal.valueOf(1000));
        request.setInterestedInJapan(true);
        request.setProjectTypeExperience("Web");
        request.setCertificates(List.of(new com.skillbridge.dto.admin.request.CertificateRequest("AWS", "AWS", null, null)));
        request.setOtherSkills(List.of(1, 2));

        Engineer savedEngineer = createEngineer(1, "John Doe", "john@example.com");
        when(engineerRepository.save(any(Engineer.class))).thenReturn(savedEngineer);

        EngineerResponseDTO response = adminEngineerService.createEngineer(request);

        assertEquals("John Doe", response.getFullName());
        verify(engineerRepository, times(1)).save(any(Engineer.class));
        verify(certificateRepository, times(1)).save(any(Certificate.class));
        verify(engineerSkillRepository, times(2)).save(any(EngineerSkill.class));
    }

    @Test
    @DisplayName("updateEngineer - throws when email conflicts with another engineer")
    void testUpdateEngineer_EmailConflict() {
        Engineer existingEngineer = createEngineer(1, "John", "john@example.com");
        UpdateEngineerRequest request = new UpdateEngineerRequest();
        request.setEmail("john@example.com");

        when(engineerRepository.findById(1)).thenReturn(Optional.of(existingEngineer));
        when(engineerRepository.findByEmail("john@example.com")).thenReturn(Optional.of(createEngineer(2, "Other", "john@example.com")));

        assertThrows(RuntimeException.class, () -> adminEngineerService.updateEngineer(1, request));
    }

    @Test
    @DisplayName("deleteEngineer - throws when engineer has active engagements")
    void testDeleteEngineer_WithEngagements() {
        when(engineerRepository.findById(1)).thenReturn(Optional.of(sampleEngineer));
        com.skillbridge.entity.contract.SOWEngagedEngineerBase base = new com.skillbridge.entity.contract.SOWEngagedEngineerBase();
        base.setEngineerId(1);
        when(sowEngagedEngineerBaseRepository.findAll()).thenReturn(List.of(base));

        assertThrows(RuntimeException.class, () -> adminEngineerService.deleteEngineer(1));
        verify(engineerRepository, never()).deleteById(anyInt());
    }

    private Engineer createEngineer(Integer id, String fullName, String email) {
        Engineer engineer = new Engineer();
        engineer.setId(id);
        engineer.setFullName(fullName);
        engineer.setEmail(email);
        engineer.setPhone("0123456789");
        engineer.setGender("Male");
        engineer.setDateOfBirth(LocalDate.of(1990, 1, 1));
        engineer.setYearsExperience(5);
        engineer.setSeniority("Senior");
        engineer.setPrimarySkill("Java");
        engineer.setSalaryExpectation(BigDecimal.valueOf(1000));
        engineer.setLocation("Hanoi");
        engineer.setStatus("AVAILABLE");
        engineer.setSummary("Summary");
        engineer.setIntroduction("Intro");
        engineer.setLanguageSummary("English");
        engineer.setProfileImageUrl("http://example.com/avatar.png");
        engineer.setInterestedInJapan(true);
        engineer.setProjectTypeExperience("Web");
        engineer.setCreatedAt(LocalDateTime.now());
        engineer.setUpdatedAt(LocalDateTime.now());
        return engineer;
    }
}

