package com.skillbridge.controller.api.admin;

import com.skillbridge.dto.admin.request.CreateUserRequest;
import com.skillbridge.dto.admin.request.UpdateUserRequest;
import com.skillbridge.dto.admin.request.UserListRequest;
import com.skillbridge.dto.admin.response.UserListResponseDTO;
import com.skillbridge.dto.admin.response.UserResponseDTO;
import com.skillbridge.service.admin.AdminUserService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController controller;

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
    @DisplayName("getUsers - success returns DTO")
    void testGetUsers_Success() {
        UserListResponseDTO dto = new UserListResponseDTO();
        when(adminUserService.getUsers(any(UserListRequest.class))).thenReturn(dto);

        ResponseEntity<?> response = controller.getUsers(0, 10, null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("getUsers - exception returns 500")
    void testGetUsers_Exception() {
        when(adminUserService.getUsers(any(UserListRequest.class))).thenThrow(new RuntimeException("Failure"));

        ResponseEntity<?> response = controller.getUsers(0, 10, null, null, null);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Failed to get users: Failure", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("getUserById - success")
    void testGetUserById_Success() {
        UserResponseDTO dto = new UserResponseDTO();
        when(adminUserService.getUserById(1)).thenReturn(dto);

        ResponseEntity<?> response = controller.getUserById(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("getUserById - runtime exception returns 404")
    void testGetUserById_NotFound() {
        when(adminUserService.getUserById(1)).thenThrow(new RuntimeException("Not found"));

        ResponseEntity<?> response = controller.getUserById(1);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not found", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("createUser - success returns 201")
    void testCreateUser_Success() {
        CreateUserRequest request = new CreateUserRequest("Full", "SALES_MANAGER", "email@example.com", "123");
        UserResponseDTO dto = new UserResponseDTO();
        when(adminUserService.createUser(request)).thenReturn(dto);

        ResponseEntity<?> response = controller.createUser(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("createUser - runtime exception returns 400")
    void testCreateUser_RuntimeException() {
        CreateUserRequest request = new CreateUserRequest("Full", "SALES_MANAGER", "email@example.com", "123");
        when(adminUserService.createUser(request)).thenThrow(new RuntimeException("Duplicate"));

        ResponseEntity<?> response = controller.createUser(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Duplicate", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("updateUser - success")
    void testUpdateUser_Success() {
        UpdateUserRequest request = new UpdateUserRequest("Full", "SALES_REP", "123");
        UserResponseDTO dto = new UserResponseDTO();
        when(adminUserService.updateUser(eq(1), eq(request))).thenReturn(dto);

        ResponseEntity<?> response = controller.updateUser(1, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("updateUser - runtime exception returns 400")
    void testUpdateUser_RuntimeException() {
        UpdateUserRequest request = new UpdateUserRequest("Full", "SALES_REP", "123");
        when(adminUserService.updateUser(eq(1), eq(request))).thenThrow(new RuntimeException("Invalid"));

        ResponseEntity<?> response = controller.updateUser(1, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid", extractMessage(response.getBody()));
    }

    @Test
    @DisplayName("deleteUser - success returns 204")
    void testDeleteUser_Success() {
        ResponseEntity<?> response = controller.deleteUser(1);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    @DisplayName("deleteUser - runtime exception returns 400")
    void testDeleteUser_RuntimeException() {
        doThrow(new RuntimeException("Cannot delete")).when(adminUserService).deleteUser(1);

        ResponseEntity<?> response = controller.deleteUser(1);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Cannot delete", extractMessage(response.getBody()));
    }
}

