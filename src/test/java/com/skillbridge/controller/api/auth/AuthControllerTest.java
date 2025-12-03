package com.skillbridge.controller.api.auth;

import com.skillbridge.dto.auth.request.ChangePasswordRequest;
import com.skillbridge.dto.auth.request.ForgotPasswordRequest;
import com.skillbridge.dto.auth.request.LoginRequest;
import com.skillbridge.dto.auth.request.ResetPasswordRequest;
import com.skillbridge.dto.auth.response.ChangePasswordResponse;
import com.skillbridge.dto.auth.response.ForgotPasswordResponse;
import com.skillbridge.dto.auth.response.LoginResponse;
import com.skillbridge.dto.auth.response.LogoutResponse;
import com.skillbridge.dto.auth.response.ResetPasswordResponse;
import com.skillbridge.service.auth.AuthService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("login - success returns 200 and LoginResponse")
    void testLogin_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken("dummy-token");
        when(authService.login(request)).thenReturn(loginResponse);

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof LoginResponse);
        LoginResponse body = (LoginResponse) response.getBody();
        assertEquals("dummy-token", body.getToken());
        verify(authService).login(request);
    }

    @Test
    @DisplayName("login - runtime exception returns 401 with message")
    void testLogin_Failure() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong");

        when(authService.login(request)).thenThrow(new RuntimeException("Invalid credentials"));

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof AuthController.ErrorResponse);
        AuthController.ErrorResponse body = (AuthController.ErrorResponse) response.getBody();
        assertEquals("Invalid credentials", body.getMessage());
    }

    @Test
    @DisplayName("forgotPassword - service succeeds returns generic success message")
    void testForgotPassword_Success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@example.com");
        doNothing().when(authService).requestPasswordReset("user@example.com");

        ResponseEntity<ForgotPasswordResponse> response = authController.forgotPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ForgotPasswordResponse body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("If an account exists with this email, a password reset link has been sent.",
                body.getMessage());
        verify(authService).requestPasswordReset("user@example.com");
    }

    @Test
    @DisplayName("forgotPassword - exception still returns generic success message")
    void testForgotPassword_Exception() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@example.com");
        doThrow(new RuntimeException("failure"))
                .when(authService).requestPasswordReset("user@example.com");

        ResponseEntity<ForgotPasswordResponse> response = authController.forgotPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ForgotPasswordResponse body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("If an account exists with this email, a password reset link has been sent.",
                body.getMessage());
    }

    @Test
    @DisplayName("changePassword - missing Authorization header returns 401")
    void testChangePassword_MissingAuthHeader() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<?> response = authController.changePassword(request, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        AuthController.ErrorResponse body = (AuthController.ErrorResponse) response.getBody();
        assertEquals("Authorization token is required", body.getMessage());
        verify(authService, never()).changePassword(anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("changePassword - malformed Authorization header returns 401")
    void testChangePassword_InvalidAuthHeader() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Invalid");

        ResponseEntity<?> response = authController.changePassword(request, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        AuthController.ErrorResponse body = (AuthController.ErrorResponse) response.getBody();
        assertEquals("Authorization token is required", body.getMessage());
        verify(authService, never()).changePassword(anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("changePassword - userId null returns 401 invalid token")
    void testChangePassword_UserIdNull() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer abc.def");
        when(jwtTokenProvider.getUserIdFromToken("abc.def")).thenReturn(null);

        ResponseEntity<?> response = authController.changePassword(request, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        AuthController.ErrorResponse body = (AuthController.ErrorResponse) response.getBody();
        assertEquals("Invalid or expired token", body.getMessage());
        verify(authService, never()).changePassword(anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("changePassword - new and confirm mismatch returns 400")
    void testChangePassword_PasswordMismatch() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old");
        request.setNewPassword("new1");
        request.setConfirmPassword("new2");

        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtTokenProvider.getUserIdFromToken("token")).thenReturn(5);

        ResponseEntity<?> response = authController.changePassword(request, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        AuthController.ErrorResponse body = (AuthController.ErrorResponse) response.getBody();
        assertEquals("New password and confirm password do not match", body.getMessage());
        verify(authService, never()).changePassword(anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("changePassword - success returns 200 and response body")
    void testChangePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old");
        request.setNewPassword("newpass");
        request.setConfirmPassword("newpass");

        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtTokenProvider.getUserIdFromToken("token")).thenReturn(10);

        ResponseEntity<?> response = authController.changePassword(request, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ChangePasswordResponse);
        ChangePasswordResponse body = (ChangePasswordResponse) response.getBody();
        assertTrue(body.isSuccess());
        assertEquals("Password changed successfully", body.getMessage());
        verify(authService).changePassword(10, "old", "newpass");
    }

    @Test
    @DisplayName("changePassword - runtime exception from service returns 400")
    void testChangePassword_RuntimeException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old");
        request.setNewPassword("newpass");
        request.setConfirmPassword("newpass");

        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtTokenProvider.getUserIdFromToken("token")).thenReturn(10);
        doThrow(new RuntimeException("invalid")).when(authService).changePassword(10, "old", "newpass");

        ResponseEntity<?> response = authController.changePassword(request, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        AuthController.ErrorResponse body = (AuthController.ErrorResponse) response.getBody();
        assertEquals("invalid", body.getMessage());
    }

    @Test
    @DisplayName("resetPassword - new and confirm mismatch returns 400")
    void testResetPassword_PasswordMismatch() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token");
        request.setNewPassword("new1");
        request.setConfirmPassword("new2");

        ResponseEntity<?> response = authController.resetPassword(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        AuthController.ErrorResponse body = (AuthController.ErrorResponse) response.getBody();
        assertEquals("New password and confirm password do not match", body.getMessage());
        verify(authService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    @DisplayName("resetPassword - success returns ResetPasswordResponse")
    void testResetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token");
        request.setNewPassword("newpass");
        request.setConfirmPassword("newpass");

        ResponseEntity<?> response = authController.resetPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ResetPasswordResponse);
        ResetPasswordResponse body = (ResetPasswordResponse) response.getBody();
        assertTrue(body.isSuccess());
        assertEquals("Password reset successfully", body.getMessage());
        verify(authService).resetPassword("token", "newpass");
    }

    @Test
    @DisplayName("resetPassword - runtime exception returns 400")
    void testResetPassword_RuntimeException() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token");
        request.setNewPassword("newpass");
        request.setConfirmPassword("newpass");

        doThrow(new RuntimeException("invalid")).when(authService).resetPassword("token", "newpass");

        ResponseEntity<?> response = authController.resetPassword(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        AuthController.ErrorResponse body = (AuthController.ErrorResponse) response.getBody();
        assertEquals("invalid", body.getMessage());
    }

    @Test
    @DisplayName("logout - always returns success message")
    void testLogout() {
        ResponseEntity<LogoutResponse> response = authController.logout(httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        LogoutResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Logged out successfully", body.getMessage());
    }
}

