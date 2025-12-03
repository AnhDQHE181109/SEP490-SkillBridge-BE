package com.skillbridge.controller.api.sales;

import com.skillbridge.dto.sales.response.ErrorResponse;
import com.skillbridge.dto.sales.response.PresignedUrlResponse;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.common.DocumentPermissionService;
import com.skillbridge.service.common.S3Service;
import com.skillbridge.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesDocumentControllerTest {

    @Mock
    private S3Service s3Service;

    @Mock
    private DocumentPermissionService documentPermissionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private SalesDocumentController controller;

    @Test
    @DisplayName("getPresignedUrl - currentUser null → 401")
    void testGetPresignedUrl_Unauthenticated() {
        when(authentication.isAuthenticated()).thenReturn(false);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<?> response = controller.getPresignedUrl("key", authentication, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(documentPermissionService, s3Service);
    }

    @Test
    @DisplayName("getPresignedUrl - role not sales → 403")
    void testGetPresignedUrl_ForbiddenRole() {
        User user = new User();
        user.setId(1);
        user.setRole("ENGINEER");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.getPresignedUrl("key", authentication, httpServletRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(documentPermissionService, s3Service);
    }

    @Test
    @DisplayName("getPresignedUrl - không có permission document → 403 với ErrorResponse")
    void testGetPresignedUrl_NoPermission() {
        User user = new User();
        user.setId(1);
        user.setRole("SALES_REP");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("sales@example.com");
        when(userRepository.findByEmail("sales@example.com")).thenReturn(Optional.of(user));

        when(documentPermissionService.hasPermission("doc-key", user)).thenReturn(false);

        ResponseEntity<?> response = controller.getPresignedUrl("doc-key", authentication, httpServletRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertTrue(body.getMessage().contains("Access denied"));
        verify(s3Service, never()).getPresignedUrl(anyString(), anyInt());
    }

    @Test
    @DisplayName("getPresignedUrl - có permission → trả về 200 với PresignedUrlResponse")
    void testGetPresignedUrl_Success() {
        User user = new User();
        user.setId(1);
        user.setRole("SALES_MANAGER");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("manager@example.com");
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(user));

        when(documentPermissionService.hasPermission("doc-key", user)).thenReturn(true);
        when(s3Service.getPresignedUrl("doc-key", 10)).thenReturn("http://signed-url");

        ResponseEntity<?> response = controller.getPresignedUrl("doc-key", authentication, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof PresignedUrlResponse);
        PresignedUrlResponse body = (PresignedUrlResponse) response.getBody();
        assertEquals("http://signed-url", body.getPresignedUrl());
        assertEquals("doc-key", body.getS3Key());
        assertEquals(10, body.getExpirationMinutes());
    }

    @Test
    @DisplayName("getPresignedUrl - Exception từ s3Service → 500 với ErrorResponse")
    void testGetPresignedUrl_Exception() {
        User user = new User();
        user.setId(1);
        user.setRole("SALES_MANAGER");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("manager@example.com");
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(user));

        when(documentPermissionService.hasPermission("doc-key", user)).thenReturn(true);
        when(s3Service.getPresignedUrl("doc-key", 10)).thenThrow(new RuntimeException("S3 error"));

        ResponseEntity<?> response = controller.getPresignedUrl("doc-key", authentication, httpServletRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertTrue(body.getMessage().contains("Failed to generate presigned URL"));
    }

    @Test
    @DisplayName("deleteDocument - currentUser null → 401")
    void testDeleteDocument_Unauthenticated() {
        when(authentication.isAuthenticated()).thenReturn(false);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<?> response = controller.deleteDocument("doc-key", authentication, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(documentPermissionService, s3Service);
    }

    @Test
    @DisplayName("deleteDocument - không có quyền delete → 403 với ErrorResponse")
    void testDeleteDocument_NoPermission() {
        User user = new User();
        user.setId(1);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        when(documentPermissionService.canDelete("doc-key", user)).thenReturn(false);

        ResponseEntity<?> response = controller.deleteDocument("doc-key", authentication, httpServletRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertTrue(body.getMessage().contains("Access denied"));
        verify(s3Service, never()).deleteFile(anyString());
    }

    @Test
    @DisplayName("deleteDocument - có quyền delete → gọi s3Service.deleteFile và trả về 200")
    void testDeleteDocument_Success() {
        User user = new User();
        user.setId(1);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        when(documentPermissionService.canDelete("doc-key", user)).thenReturn(true);

        ResponseEntity<?> response = controller.deleteDocument("doc-key", authentication, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(s3Service).deleteFile("doc-key");
    }

    @Test
    @DisplayName("deleteDocument - Exception từ s3Service → 500 với ErrorResponse")
    void testDeleteDocument_Exception() {
        User user = new User();
        user.setId(1);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        when(documentPermissionService.canDelete("doc-key", user)).thenReturn(true);
        doThrow(new RuntimeException("S3 delete error")).when(s3Service).deleteFile("doc-key");

        ResponseEntity<?> response = controller.deleteDocument("doc-key", authentication, httpServletRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertTrue(body.getMessage().contains("Failed to delete document"));
    }
}


