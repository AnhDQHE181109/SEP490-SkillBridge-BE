package com.skillbridge.service.admin;

import com.skillbridge.dto.admin.response.AdminDashboardSummaryDTO;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.engineer.EngineerRepository;
import com.skillbridge.repository.engineer.ProjectTypeRepository;
import com.skillbridge.repository.engineer.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminDashboardService}
 */
@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private EngineerRepository engineerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private ProjectTypeRepository projectTypeRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @BeforeEach
    void setUp() {
        // MockitoExtension handles setup
    }

    @Test
    @DisplayName("getDashboardSummary - aggregates counts from repositories")
    void testGetDashboardSummary_AggregatesCounts() {
        // Arrange
        when(engineerRepository.countByStatus("AVAILABLE")).thenReturn(6L);
        when(engineerRepository.count()).thenReturn(10L);
        when(userRepository.countByIsActive(true)).thenReturn(8L);
        when(userRepository.countByIsActive(false)).thenReturn(2L);
        when(skillRepository.count()).thenReturn(15L);
        when(projectTypeRepository.count()).thenReturn(5L);

        // Act
        AdminDashboardSummaryDTO result = adminDashboardService.getDashboardSummary();

        // Assert
        assertEquals(6, result.getEngineers().getActive());
        assertEquals(4, result.getEngineers().getInactive());
        assertEquals(8, result.getSystemUsers().getActive());
        assertEquals(2, result.getSystemUsers().getInactive());
        assertEquals(15, result.getSkills().getTotal());
        assertEquals(5, result.getProjectTypes().getTotal());

        verify(engineerRepository).countByStatus("AVAILABLE");
        verify(engineerRepository).count();
        verify(userRepository).countByIsActive(true);
        verify(userRepository).countByIsActive(false);
        verify(skillRepository).count();
        verify(projectTypeRepository).count();
    }

    @Test
    @DisplayName("getDashboardSummary - handles zero counts gracefully")
    void testGetDashboardSummary_ZeroCounts() {
        // Arrange
        when(engineerRepository.countByStatus("AVAILABLE")).thenReturn(0L);
        when(engineerRepository.count()).thenReturn(0L);
        when(userRepository.countByIsActive(true)).thenReturn(0L);
        when(userRepository.countByIsActive(false)).thenReturn(0L);
        when(skillRepository.count()).thenReturn(0L);
        when(projectTypeRepository.count()).thenReturn(0L);

        // Act
        AdminDashboardSummaryDTO result = adminDashboardService.getDashboardSummary();

        // Assert
        assertEquals(0, result.getEngineers().getActive());
        assertEquals(0, result.getEngineers().getInactive());
        assertEquals(0, result.getSystemUsers().getActive());
        assertEquals(0, result.getSystemUsers().getInactive());
        assertEquals(0, result.getSkills().getTotal());
        assertEquals(0, result.getProjectTypes().getTotal());
    }
}

