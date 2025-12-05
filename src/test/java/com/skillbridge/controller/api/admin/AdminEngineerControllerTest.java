package com.skillbridge.controller.api.admin;

import com.skillbridge.dto.admin.request.CreateEngineerRequest;
import com.skillbridge.dto.admin.request.UpdateEngineerRequest;
import com.skillbridge.dto.admin.response.EngineerListResponse;
import com.skillbridge.dto.admin.response.EngineerResponseDTO;
import com.skillbridge.service.admin.AdminEngineerService;
import com.skillbridge.service.common.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminEngineerControllerTest {

    @Mock
    private AdminEngineerService adminEngineerService;

    @Mock
    private S3Service s3Service;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private AdminEngineerController adminEngineerController;

    private String extractMessage(Object obj) {
        try {
            var field = obj.getClass().getDeclaredField("message");
            field.setAccessible(true);
            return (String) field.get(obj);
        } catch (Exception e) {
            throw new AssertionError("Unable to read error message", e);
        }
    }

    @Test
    @DisplayName("getAllEngineers - success returns list response")
    void testGetAllEngineers_Success() {
        EngineerListResponse responseBody = new EngineerListResponse();
        when(adminEngineerService.getAllEngineers(0, 20, null)).thenReturn(responseBody);

        ResponseEntity<?> response = adminEngineerController.getAllEngineers(0, 20, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(responseBody, response.getBody());
    }

    @Test
    @DisplayName("getAllEngineers - exception returns 500")
    void testGetAllEngineers_Exception() {
        when(adminEngineerService.getAllEngineers(anyInt(), anyInt(), any()))
                .thenThrow(new RuntimeException("DB error"));

        ResponseEntity<?> response = adminEngineerController.getAllEngineers(0, 20, null);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(extractMessage(response.getBody()).contains("Failed to get engineers: DB error"));
    }

    @Test
    @DisplayName("getEngineer - success")
    void testGetEngineer_Success() {
        EngineerResponseDTO dto = new EngineerResponseDTO();
        when(adminEngineerService.getEngineerById(1)).thenReturn(dto);

        ResponseEntity<?> response = adminEngineerController.getEngineer(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("getEngineer - runtime exception returns 404")
    void testGetEngineer_NotFound() {
        when(adminEngineerService.getEngineerById(1)).thenThrow(new RuntimeException("Not found"));
        ResponseEntity<?> response = adminEngineerController.getEngineer(1);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not found", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("createEngineer - success returns 201")
    void testCreateEngineer_Success() {
        CreateEngineerRequest request = new CreateEngineerRequest();
        EngineerResponseDTO dto = new EngineerResponseDTO();
        when(adminEngineerService.createEngineer(request)).thenReturn(dto);

        ResponseEntity<?> response = adminEngineerController.createEngineer(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("createEngineer - runtime exception returns 400")
    void testCreateEngineer_RuntimeException() {
        CreateEngineerRequest request = new CreateEngineerRequest();
        when(adminEngineerService.createEngineer(request)).thenThrow(new RuntimeException("Duplicate email"));

        ResponseEntity<?> response = adminEngineerController.createEngineer(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Duplicate email", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("updateEngineer - success")
    void testUpdateEngineer_Success() {
        UpdateEngineerRequest request = new UpdateEngineerRequest();
        EngineerResponseDTO dto = new EngineerResponseDTO();
        when(adminEngineerService.updateEngineer(1, request)).thenReturn(dto);

        ResponseEntity<?> response = adminEngineerController.updateEngineer(1, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("updateEngineer - runtime exception returns 400")
    void testUpdateEngineer_RuntimeException() {
        UpdateEngineerRequest request = new UpdateEngineerRequest();
        when(adminEngineerService.updateEngineer(1, request)).thenThrow(new RuntimeException("Invalid"));

        ResponseEntity<?> response = adminEngineerController.updateEngineer(1, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("deleteEngineer - success returns 204")
    void testDeleteEngineer_Success() {
        ResponseEntity<?> response = adminEngineerController.deleteEngineer(1);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    @DisplayName("deleteEngineer - runtime exception returns 400")
    void testDeleteEngineer_RuntimeException() {
        doThrow(new RuntimeException("Cannot delete")).when(adminEngineerService).deleteEngineer(1);
        ResponseEntity<?> response = adminEngineerController.deleteEngineer(1);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Cannot delete", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("uploadProfileImage - empty file returns 400")
    void testUploadProfileImage_EmptyFile() {
        when(multipartFile.isEmpty()).thenReturn(true);
        ResponseEntity<?> response = adminEngineerController.uploadProfileImage(multipartFile);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("File is empty", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("uploadProfileImage - invalid content type returns 400")
    void testUploadProfileImage_InvalidContentType() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");

        ResponseEntity<?> response = adminEngineerController.uploadProfileImage(multipartFile);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("File must be an image", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("uploadProfileImage - size too large returns 400")
    void testUploadProfileImage_SizeTooLarge() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getSize()).thenReturn(6 * 1024 * 1024L);

        ResponseEntity<?> response = adminEngineerController.uploadProfileImage(multipartFile);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Image size must be less than 5MB", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("uploadProfileImage - S3 service not configured returns 500")
    void testUploadProfileImage_S3NotConfigured() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getSize()).thenReturn(1024L);

        // Force s3Service to null via reflection
        setField(adminEngineerController, "s3Service", null);

        ResponseEntity<?> response = adminEngineerController.uploadProfileImage(multipartFile);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("S3 service is not configured", extractMessage(response.getBody()));

        // Restore s3Service mock for other tests
        setField(adminEngineerController, "s3Service", s3Service);
    }

    @Test
    @DisplayName("uploadProfileImage - success returns URLs")
    void testUploadProfileImage_Success() throws IOException {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(s3Service.uploadPublicFile(multipartFile, "engineers/profiles")).thenReturn("s3-key");
        when(s3Service.getPresignedUrl("s3-key", 24 * 60)).thenReturn("https://presigned");

        ResponseEntity<?> response = adminEngineerController.uploadProfileImage(multipartFile);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("https://presigned", body.get("imageUrl"));
        assertEquals("s3-key", body.get("s3Key"));
    }

    @Test
    @DisplayName("uploadProfileImage - IOException returns 500")
    void testUploadProfileImage_IOException() throws IOException {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(s3Service.uploadPublicFile(multipartFile, "engineers/profiles"))
                .thenThrow(new IOException("S3 down"));

        ResponseEntity<?> response = adminEngineerController.uploadProfileImage(multipartFile);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(extractMessage(response.getBody()).contains("Failed to upload image: S3 down"));
    }
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new AssertionError("Unable to set field " + fieldName, e);
        }
    }
}

