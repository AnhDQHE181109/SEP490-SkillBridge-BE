package com.skillbridge.service.admin;

import com.skillbridge.dto.admin.response.AdminDashboardSummaryDTO;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.engineer.EngineerRepository;
import com.skillbridge.repository.engineer.ProjectTypeRepository;
import com.skillbridge.repository.engineer.SkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin Dashboard Service
 * Handles business logic for admin dashboard summary statistics
 */
@Service
@Transactional
public class AdminDashboardService {

    @Autowired
    private EngineerRepository engineerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private ProjectTypeRepository projectTypeRepository;

    /**
     * Get dashboard summary statistics
     * @return AdminDashboardSummaryDTO with counts for engineers, users, skills, and project types
     */
    public AdminDashboardSummaryDTO getDashboardSummary() {
        AdminDashboardSummaryDTO summary = new AdminDashboardSummaryDTO();

        // Calculate engineer counts
        Long activeEngineers = engineerRepository.countByStatus("AVAILABLE");
        Long totalEngineers = engineerRepository.count();
        Long inactiveEngineers = totalEngineers - activeEngineers;

        AdminDashboardSummaryDTO.EngineerSummary engineerSummary = 
            new AdminDashboardSummaryDTO.EngineerSummary(
                activeEngineers.intValue(),
                inactiveEngineers.intValue()
            );
        summary.setEngineers(engineerSummary);

        // Calculate user counts
        Long activeUsers = userRepository.countByIsActive(true);
        Long inactiveUsers = userRepository.countByIsActive(false);

        AdminDashboardSummaryDTO.SystemUserSummary systemUserSummary = 
            new AdminDashboardSummaryDTO.SystemUserSummary(
                activeUsers.intValue(),
                inactiveUsers.intValue()
            );
        summary.setSystemUsers(systemUserSummary);

        // Calculate skills total count
        Long totalSkills = skillRepository.count();

        AdminDashboardSummaryDTO.SkillSummary skillSummary = 
            new AdminDashboardSummaryDTO.SkillSummary(totalSkills.intValue());
        summary.setSkills(skillSummary);

        // Calculate project types total count
        Long totalProjectTypes = projectTypeRepository.count();

        AdminDashboardSummaryDTO.ProjectTypeSummary projectTypeSummary = 
            new AdminDashboardSummaryDTO.ProjectTypeSummary(totalProjectTypes.intValue());
        summary.setProjectTypes(projectTypeSummary);

        return summary;
    }
}

