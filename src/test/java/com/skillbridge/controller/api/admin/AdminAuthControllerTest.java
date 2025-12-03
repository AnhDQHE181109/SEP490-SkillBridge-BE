package com.skillbridge.controller.api.admin;

import com.skillbridge.dto.auth.request.LoginRequest;
import com.skillbridge.dto.auth.response.LoginResponse;
import com.skillbridge.service.auth.AdminAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthControllerTest {

    @Mock
    private AdminAuthService adminAuthService;

    @InjectMocks
    private AdminAuthController adminAuthController;

    @Test
    @DisplayName("login - success returns 200 with LoginResponse")
    void testLogin_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@example.com");
        request.setPassword("password");

        LoginResponse responseBody = new LoginResponse();
        responseBody.setToken("token");

        when(adminAuthService.login(request)).thenReturn(responseBody);

        ResponseEntity<?> response = adminAuthController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof LoginResponse);
        assertEquals("token", ((LoginResponse) response.getBody()).getToken());
    }

    @Test
    @DisplayName("login - runtime exception returns 401 with message")
    void testLogin_InvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@example.com");
        request.setPassword("wrong");

        when(adminAuthService.login(request)).thenThrow(new RuntimeException("Invalid credentials"));

        ResponseEntity<?> response = adminAuthController.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", extractMessage(response.getBody()));
    }

    private String extractMessage(Object errorResponse) {
        try {
            var field = errorResponse.getClass().getDeclaredField("message");
            field.setAccessible(true);
            return (String) field.get(errorResponse);
        } catch (Exception e) {
            throw new AssertionError("Unable to extract message", e);
        }
    }
}

