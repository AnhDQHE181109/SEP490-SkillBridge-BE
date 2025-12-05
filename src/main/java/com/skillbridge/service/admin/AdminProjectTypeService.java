package com.skillbridge.service.admin;

import com.skillbridge.dto.admin.request.CreateProjectTypeRequest;
import com.skillbridge.dto.admin.request.UpdateProjectTypeRequest;
import com.skillbridge.dto.admin.response.ProjectTypeListResponse;
import com.skillbridge.dto.admin.response.ProjectTypeResponseDTO;
import com.skillbridge.entity.engineer.ProjectType;
import com.skillbridge.repository.engineer.ProjectTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Project Type Service
 * Handles all project type operations for Admin Master Data
 */
@Service
@Transactional
public class AdminProjectTypeService {

    @Autowired
    private ProjectTypeRepository projectTypeRepository;

    /**
     * Get all project types with pagination and search
     */
    public ProjectTypeListResponse getAllProjectTypes(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProjectType> projectTypePage;

        if (search != null && !search.trim().isEmpty()) {
            projectTypePage = projectTypeRepository.searchProjectTypes(search.trim(), pageable);
        } else {
            projectTypePage = projectTypeRepository.findAll(pageable);
        }

        List<ProjectTypeResponseDTO> content = projectTypePage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        ProjectTypeListResponse.PageInfo pageInfo = new ProjectTypeListResponse.PageInfo(
                projectTypePage.getNumber(),
                projectTypePage.getSize(),
                projectTypePage.getTotalElements(),
                projectTypePage.getTotalPages()
        );

        ProjectTypeListResponse response = new ProjectTypeListResponse();
        response.setContent(content);
        response.setPage(pageInfo);

        return response;
    }

    /**
     * Create a new project type
     */
    public ProjectTypeResponseDTO createProjectType(CreateProjectTypeRequest request) {
        // Check if project type name already exists
        projectTypeRepository.findByName(request.getName())
                .ifPresent(s -> {
                    throw new RuntimeException("Project type name already exists: " + request.getName());
                });

        // Create project type
        ProjectType projectType = new ProjectType();
        projectType.setName(request.getName());
        projectType.setDescription(request.getDescription());

        projectType = projectTypeRepository.save(projectType);

        return convertToDTO(projectType);
    }

    /**
     * Update a project type
     */
    public ProjectTypeResponseDTO updateProjectType(Integer projectTypeId, UpdateProjectTypeRequest request) {
        ProjectType projectType = projectTypeRepository.findById(projectTypeId)
                .orElseThrow(() -> new RuntimeException("Project type not found"));

        // Check if new name conflicts with existing project type (excluding current project type)
        projectTypeRepository.findByName(request.getName())
                .ifPresent(pt -> {
                    if (!pt.getId().equals(projectTypeId)) {
                        throw new RuntimeException("Project type name already exists: " + request.getName());
                    }
                });

        // Update project type
        projectType.setName(request.getName());
        projectType.setDescription(request.getDescription());

        projectType = projectTypeRepository.save(projectType);

        return convertToDTO(projectType);
    }

    /**
     * Delete a project type
     */
    public void deleteProjectType(Integer projectTypeId) {
        ProjectType projectType = projectTypeRepository.findById(projectTypeId)
                .orElseThrow(() -> new RuntimeException("Project type not found"));

        // TODO: Check if project type is in use by opportunities/projects before deletion
        // For now, we'll allow deletion. In the future, add validation:
        // if (isProjectTypeInUse(projectTypeId)) {
        //     throw new RuntimeException("Cannot delete project type. It is currently in use by opportunities/projects.");
        // }

        // Delete project type
        projectTypeRepository.deleteById(projectTypeId);
    }

    /**
     * Check if a project type is in use by opportunities/projects
     * TODO: Implement this method when project_type relationship is added to opportunities table
     */
    public boolean isProjectTypeInUse(Integer projectTypeId) {
        // TODO: Check if project type is used in opportunities table
        // For now, return false (no relationship exists yet)
        return false;
    }

    /**
     * Convert ProjectType entity to ProjectTypeResponseDTO
     */
    private ProjectTypeResponseDTO convertToDTO(ProjectType projectType) {
        ProjectTypeResponseDTO dto = new ProjectTypeResponseDTO();
        dto.setId(projectType.getId());
        dto.setName(projectType.getName());
        dto.setDescription(projectType.getDescription());
        return dto;
    }
}

