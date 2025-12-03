package com.skillbridge.controller.api.admin;

import com.skillbridge.dto.admin.response.AdminDashboardSummaryDTO;
import com.skillbridge.service.admin.AdminDashboardService;
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
class AdminDashboardControllerTest {

    @Mock
    private AdminDashboardService adminDashboardService;

    @InjectMocks
    private AdminDashboardController adminDashboardController;

    @Test
    @DisplayName("getDashboardSummary - success returns summary DTO")
    void testGetDashboardSummary_Success() {
        AdminDashboardSummaryDTO summary = new AdminDashboardSummaryDTO();
        when(adminDashboardService.getDashboardSummary()).thenReturn(summary);

        ResponseEntity<?> response = adminDashboardController.getDashboardSummary();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof AdminDashboardSummaryDTO);
    }

    @Test
    @DisplayName("getDashboardSummary - exception returns 500 with message")
    void testGetDashboardSummary_Exception() {
        when(adminDashboardService.getDashboardSummary()).thenThrow(new RuntimeException("DB down"));

        ResponseEntity<?> response = adminDashboardController.getDashboardSummary();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Failed to get dashboard summary: DB down", extractMessage(response.getBody()));
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

