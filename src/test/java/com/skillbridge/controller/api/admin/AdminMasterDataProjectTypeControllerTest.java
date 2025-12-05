package com.skillbridge.controller.api.admin;

import com.skillbridge.dto.admin.request.CreateProjectTypeRequest;
import com.skillbridge.dto.admin.request.UpdateProjectTypeRequest;
import com.skillbridge.dto.admin.response.ProjectTypeListResponse;
import com.skillbridge.dto.admin.response.ProjectTypeResponseDTO;
import com.skillbridge.service.admin.AdminProjectTypeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMasterDataProjectTypeControllerTest {

    @Mock
    private AdminProjectTypeService adminProjectTypeService;

    @InjectMocks
    private AdminMasterDataProjectTypeController controller;

    private String extractMessage(Object responseBody) {
        try {
            var field = responseBody.getClass().getDeclaredField("message");
            field.setAccessible(true);
            return (String) field.get(responseBody);
        } catch (Exception e) {
            throw new AssertionError("Unable to extract message", e);
        }
    }

    @Test
    @DisplayName("getAllProjectTypes - success")
    void testGetAllProjectTypes_Success() {
        ProjectTypeListResponse listResponse = new ProjectTypeListResponse();
        when(adminProjectTypeService.getAllProjectTypes(0, 20, null)).thenReturn(listResponse);

        ResponseEntity<?> response = controller.getAllProjectTypes(0, 20, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(listResponse, response.getBody());
    }

    @Test
    @DisplayName("getAllProjectTypes - exception returns 500")
    void testGetAllProjectTypes_Exception() {
        when(adminProjectTypeService.getAllProjectTypes(0, 20, null)).thenThrow(new RuntimeException("DB down"));

        ResponseEntity<?> response = controller.getAllProjectTypes(0, 20, null);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Failed to get project types: DB down", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("createProjectType - success returns 201")
    void testCreateProjectType_Success() {
        CreateProjectTypeRequest request = new CreateProjectTypeRequest();
        ProjectTypeResponseDTO dto = new ProjectTypeResponseDTO();
        when(adminProjectTypeService.createProjectType(request)).thenReturn(dto);

        ResponseEntity<?> response = controller.createProjectType(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("createProjectType - runtime exception returns 400")
    void testCreateProjectType_RuntimeException() {
        CreateProjectTypeRequest request = new CreateProjectTypeRequest();
        when(adminProjectTypeService.createProjectType(request)).thenThrow(new RuntimeException("Duplicate"));

        ResponseEntity<?> response = controller.createProjectType(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Duplicate", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("updateProjectType - success")
    void testUpdateProjectType_Success() {
        UpdateProjectTypeRequest request = new UpdateProjectTypeRequest();
        ProjectTypeResponseDTO dto = new ProjectTypeResponseDTO();
        when(adminProjectTypeService.updateProjectType(1, request)).thenReturn(dto);

        ResponseEntity<?> response = controller.updateProjectType(1, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("updateProjectType - runtime exception returns 400")
    void testUpdateProjectType_RuntimeException() {
        UpdateProjectTypeRequest request = new UpdateProjectTypeRequest();
        when(adminProjectTypeService.updateProjectType(1, request)).thenThrow(new RuntimeException("Name exists"));

        ResponseEntity<?> response = controller.updateProjectType(1, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Name exists", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("deleteProjectType - success returns 200")
    void testDeleteProjectType_Success() {
        ResponseEntity<?> response = controller.deleteProjectType(1);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("deleteProjectType - runtime exception returns 400")
    void testDeleteProjectType_RuntimeException() {
        doThrow(new RuntimeException("In use")).when(adminProjectTypeService).deleteProjectType(1);

        ResponseEntity<?> response = controller.deleteProjectType(1);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("In use", extractMessage(response.getBody()));
    }
}

