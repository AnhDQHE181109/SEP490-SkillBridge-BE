package com.skillbridge.controller.client.document;

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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientDocumentControllerTest {

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

    @InjectMocks
    private ClientDocumentController controller;

    private User buildUser(String role) {
        User u = new User();
        u.setId(1);
        u.setRole(role);
        return u;
    }

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
    @DisplayName("getPresignedUrl - role không phải CLIENT/CLIENT_USER → 403")
    void testGetPresignedUrl_ForbiddenRole() {
        User user = buildUser("ENGINEER");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.getPresignedUrl("key", authentication, httpServletRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(documentPermissionService, s3Service);
    }

    @Test
    @DisplayName("getPresignedUrl - không có permission → 403 với ErrorResponse")
    void testGetPresignedUrl_NoPermission() {
        User user = buildUser("CLIENT");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("client@example.com");
        when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(user));

        when(documentPermissionService.hasPermission("key", user)).thenReturn(false);

        ResponseEntity<?> response = controller.getPresignedUrl("key", authentication, httpServletRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertTrue(body.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("getPresignedUrl - success → 200 với PresignedUrlResponse")
    void testGetPresignedUrl_Success() {
        User user = buildUser("CLIENT_USER");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("client2@example.com");
        when(userRepository.findByEmail("client2@example.com")).thenReturn(Optional.of(user));

        when(documentPermissionService.hasPermission("key", user)).thenReturn(true);
        when(s3Service.getPresignedUrl("key", 10)).thenReturn("http://url");

        ResponseEntity<?> response = controller.getPresignedUrl("key", authentication, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof PresignedUrlResponse);
        PresignedUrlResponse body = (PresignedUrlResponse) response.getBody();
        assertEquals("http://url", body.getPresignedUrl());
        assertEquals("key", body.getS3Key());
        assertEquals(10, body.getExpirationMinutes());
    }

    @Test
    @DisplayName("getPresignedUrl - exception từ s3Service → 500 với ErrorResponse")
    void testGetPresignedUrl_Exception() {
        User user = buildUser("CLIENT");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("client@example.com");
        when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(user));

        when(documentPermissionService.hasPermission("key", user)).thenReturn(true);
        when(s3Service.getPresignedUrl("key", 10)).thenThrow(new RuntimeException("S3 error"));

        ResponseEntity<?> response = controller.getPresignedUrl("key", authentication, httpServletRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertTrue(body.getMessage().contains("Failed to generate presigned URL"));
    }
}


