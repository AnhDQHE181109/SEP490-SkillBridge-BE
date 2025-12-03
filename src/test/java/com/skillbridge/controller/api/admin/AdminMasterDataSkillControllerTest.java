package com.skillbridge.controller.api.admin;

import com.skillbridge.dto.admin.request.CreateSkillRequest;
import com.skillbridge.dto.admin.request.CreateSubSkillRequest;
import com.skillbridge.dto.admin.request.UpdateSkillRequest;
import com.skillbridge.dto.admin.request.UpdateSubSkillRequest;
import com.skillbridge.dto.admin.response.SkillListResponse;
import com.skillbridge.dto.admin.response.SkillResponseDTO;
import com.skillbridge.service.admin.AdminSkillService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMasterDataSkillControllerTest {

    @Mock
    private AdminSkillService adminSkillService;

    @InjectMocks
    private AdminMasterDataSkillController controller;

    private String extractMessage(Object obj) {
        try {
            var field = obj.getClass().getDeclaredField("message");
            field.setAccessible(true);
            return (String) field.get(obj);
        } catch (Exception e) {
            throw new AssertionError("Unable to extract message", e);
        }
    }

    @Test
    @DisplayName("getAllParentSkills - success")
    void testGetAllParentSkills_Success() {
        SkillListResponse responseBody = new SkillListResponse();
        when(adminSkillService.getAllParentSkills(0, 20, null)).thenReturn(responseBody);

        ResponseEntity<?> response = controller.getAllParentSkills(0, 20, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(responseBody, response.getBody());
    }

    @Test
    @DisplayName("getAllParentSkills - exception returns 500")
    void testGetAllParentSkills_Exception() {
        when(adminSkillService.getAllParentSkills(0, 20, null)).thenThrow(new RuntimeException("Failure"));

        ResponseEntity<?> response = controller.getAllParentSkills(0, 20, null);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Failed to get skills: Failure", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("getSubSkills - success returns list")
    void testGetSubSkills_Success() {
        List<SkillResponseDTO> subSkills = List.of(new SkillResponseDTO());
        when(adminSkillService.getSubSkillsByParentId(1)).thenReturn(subSkills);

        ResponseEntity<?> response = controller.getSubSkills(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(subSkills, response.getBody());
    }

    @Test
    @DisplayName("getSubSkills - runtime exception returns 404")
    void testGetSubSkills_RuntimeException() {
        when(adminSkillService.getSubSkillsByParentId(1)).thenThrow(new RuntimeException("Parent not found"));

        ResponseEntity<?> response = controller.getSubSkills(1);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Parent not found", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("createSkill - success returns 201")
    void testCreateSkill_Success() {
        CreateSkillRequest request = new CreateSkillRequest();
        SkillResponseDTO dto = new SkillResponseDTO();
        when(adminSkillService.createSkill(request)).thenReturn(dto);

        ResponseEntity<?> response = controller.createSkill(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("createSkill - runtime exception returns 400")
    void testCreateSkill_RuntimeException() {
        CreateSkillRequest request = new CreateSkillRequest();
        when(adminSkillService.createSkill(request)).thenThrow(new RuntimeException("Duplicate"));

        ResponseEntity<?> response = controller.createSkill(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Duplicate", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("createSubSkill - success returns 201")
    void testCreateSubSkill_Success() {
        CreateSubSkillRequest request = new CreateSubSkillRequest();
        SkillResponseDTO dto = new SkillResponseDTO();
        when(adminSkillService.createSubSkill(1, request)).thenReturn(dto);

        ResponseEntity<?> response = controller.createSubSkill(1, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("createSubSkill - runtime exception returns 400")
    void testCreateSubSkill_RuntimeException() {
        CreateSubSkillRequest request = new CreateSubSkillRequest();
        when(adminSkillService.createSubSkill(1, request)).thenThrow(new RuntimeException("Duplicate"));

        ResponseEntity<?> response = controller.createSubSkill(1, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Duplicate", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("updateSkill - success")
    void testUpdateSkill_Success() {
        UpdateSkillRequest request = new UpdateSkillRequest();
        SkillResponseDTO dto = new SkillResponseDTO();
        when(adminSkillService.updateSkill(1, request)).thenReturn(dto);

        ResponseEntity<?> response = controller.updateSkill(1, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("updateSkill - runtime exception returns 400")
    void testUpdateSkill_RuntimeException() {
        UpdateSkillRequest request = new UpdateSkillRequest();
        when(adminSkillService.updateSkill(1, request)).thenThrow(new RuntimeException("Invalid"));

        ResponseEntity<?> response = controller.updateSkill(1, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("updateSubSkill - success")
    void testUpdateSubSkill_Success() {
        UpdateSubSkillRequest request = new UpdateSubSkillRequest();
        SkillResponseDTO dto = new SkillResponseDTO();
        when(adminSkillService.updateSubSkill(2, request)).thenReturn(dto);

        ResponseEntity<?> response = controller.updateSubSkill(2, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("updateSubSkill - runtime exception returns 400")
    void testUpdateSubSkill_RuntimeException() {
        UpdateSubSkillRequest request = new UpdateSubSkillRequest();
        when(adminSkillService.updateSubSkill(2, request)).thenThrow(new RuntimeException("Invalid"));

        ResponseEntity<?> response = controller.updateSubSkill(2, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("deleteSkill - success")
    void testDeleteSkill_Success() {
        ResponseEntity<?> response = controller.deleteSkill(1);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("deleteSkill - runtime exception returns 400")
    void testDeleteSkill_RuntimeException() {
        doThrow(new RuntimeException("In use")).when(adminSkillService).deleteSkill(1);
        ResponseEntity<?> response = controller.deleteSkill(1);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("In use", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("deleteSubSkill - runtime exception returns 400")
    void testDeleteSubSkill_RuntimeException() {
        doThrow(new RuntimeException("In use")).when(adminSkillService).deleteSubSkill(2);
        ResponseEntity<?> response = controller.deleteSubSkill(2);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("In use", extractMessage(response.getBody()));
    }
}

