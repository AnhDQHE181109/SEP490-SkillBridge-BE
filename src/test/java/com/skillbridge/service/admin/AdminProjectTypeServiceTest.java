package com.skillbridge.service.admin;

import com.skillbridge.dto.admin.request.CreateProjectTypeRequest;
import com.skillbridge.dto.admin.request.UpdateProjectTypeRequest;
import com.skillbridge.dto.admin.response.ProjectTypeListResponse;
import com.skillbridge.dto.admin.response.ProjectTypeResponseDTO;
import com.skillbridge.entity.engineer.ProjectType;
import com.skillbridge.repository.engineer.ProjectTypeRepository;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AdminProjectTypeService}
 */
@ExtendWith(MockitoExtension.class)
class AdminProjectTypeServiceTest {

    @Mock
    private ProjectTypeRepository projectTypeRepository;

    @InjectMocks
    private AdminProjectTypeService adminProjectTypeService;

    private ProjectType projectType;

    @BeforeEach
    void setUp() {
        projectType = new ProjectType();
        projectType.setId(1);
        projectType.setName("Web Development");
        projectType.setDescription("Web projects");
    }

    @Test
    @DisplayName("getAllProjectTypes - without search uses findAll")
    void testGetAllProjectTypes_NoSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProjectType> page = new PageImpl<>(java.util.List.of(projectType), pageable, 1);
        when(projectTypeRepository.findAll(pageable)).thenReturn(page);

        ProjectTypeListResponse response = adminProjectTypeService.getAllProjectTypes(0, 10, null);

        assertEquals(1, response.getContent().size());
        assertEquals("Web Development", response.getContent().get(0).getName());
        verify(projectTypeRepository).findAll(pageable);
        verify(projectTypeRepository, never()).searchProjectTypes(anyString(), any());
    }

    @Test
    @DisplayName("getAllProjectTypes - with search uses repository search")
    void testGetAllProjectTypes_WithSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProjectType> page = new PageImpl<>(java.util.List.of(projectType), pageable, 1);
        when(projectTypeRepository.searchProjectTypes("web", pageable)).thenReturn(page);

        ProjectTypeListResponse response = adminProjectTypeService.getAllProjectTypes(0, 10, " web ");

        assertEquals(1, response.getContent().size());
        verify(projectTypeRepository).searchProjectTypes("web", pageable);
    }

    @Test
    @DisplayName("createProjectType - throws when name already exists")
    void testCreateProjectType_NameExists() {
        CreateProjectTypeRequest request = new CreateProjectTypeRequest();
        request.setName("Web Development");

        when(projectTypeRepository.findByName("Web Development")).thenReturn(Optional.of(projectType));

        assertThrows(RuntimeException.class, () -> adminProjectTypeService.createProjectType(request));
        verify(projectTypeRepository, never()).save(any(ProjectType.class));
    }

    @Test
    @DisplayName("createProjectType - saves new project type")
    void testCreateProjectType_Success() {
        CreateProjectTypeRequest request = new CreateProjectTypeRequest();
        request.setName("Mobile");
        request.setDescription("Mobile apps");

        ProjectType saved = new ProjectType();
        saved.setId(2);
        saved.setName("Mobile");
        saved.setDescription("Mobile apps");

        when(projectTypeRepository.findByName("Mobile")).thenReturn(Optional.empty());
        when(projectTypeRepository.save(any(ProjectType.class))).thenReturn(saved);

        ProjectTypeResponseDTO response = adminProjectTypeService.createProjectType(request);

        assertEquals("Mobile", response.getName());
        verify(projectTypeRepository).save(any(ProjectType.class));
    }

    @Test
    @DisplayName("updateProjectType - throws when project type not found")
    void testUpdateProjectType_NotFound() {
        when(projectTypeRepository.findById(1)).thenReturn(Optional.empty());
        UpdateProjectTypeRequest request = new UpdateProjectTypeRequest();
        assertThrows(RuntimeException.class, () -> adminProjectTypeService.updateProjectType(1, request));
    }

    @Test
    @DisplayName("updateProjectType - throws when new name conflicts with another record")
    void testUpdateProjectType_NameConflict() {
        UpdateProjectTypeRequest request = new UpdateProjectTypeRequest();
        request.setName("New Name");

        ProjectType another = new ProjectType();
        another.setId(2);
        another.setName("New Name");

        when(projectTypeRepository.findById(1)).thenReturn(Optional.of(projectType));
        when(projectTypeRepository.findByName("New Name")).thenReturn(Optional.of(another));

        assertThrows(RuntimeException.class, () -> adminProjectTypeService.updateProjectType(1, request));
    }

    @Test
    @DisplayName("deleteProjectType - throws when project type not found")
    void testDeleteProjectType_NotFound() {
        when(projectTypeRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> adminProjectTypeService.deleteProjectType(1));
    }

    @Test
    @DisplayName("deleteProjectType - deletes when exists")
    void testDeleteProjectType_Success() {
        when(projectTypeRepository.findById(1)).thenReturn(Optional.of(projectType));
        adminProjectTypeService.deleteProjectType(1);
        verify(projectTypeRepository).deleteById(1);
    }
}

