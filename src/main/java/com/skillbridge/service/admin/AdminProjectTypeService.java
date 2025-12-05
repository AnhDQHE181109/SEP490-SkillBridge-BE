package com.skillbridge.service.admin;

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

