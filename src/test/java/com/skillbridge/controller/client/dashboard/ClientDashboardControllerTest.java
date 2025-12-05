package com.skillbridge.controller.client.dashboard;

import com.skillbridge.dto.dashboard.response.ActivitiesResponseDTO;
import com.skillbridge.dto.dashboard.response.AlertsResponseDTO;
import com.skillbridge.dto.dashboard.response.DashboardSummaryDTO;
import com.skillbridge.service.dashboard.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientDashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private ClientDashboardController controller;

    @Test
    @DisplayName("getSummary - missing X-User-Id → 401")
    void testGetSummary_MissingUserId() {
        ResponseEntity<DashboardSummaryDTO> response = controller.getSummary(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(dashboardService);
    }

    @Test
    @DisplayName("getSummary - success → 200 với DashboardSummaryDTO")
    void testGetSummary_Success() {
        DashboardSummaryDTO dto = new DashboardSummaryDTO();
        when(dashboardService.getSummary(10)).thenReturn(dto);

        ResponseEntity<DashboardSummaryDTO> response = controller.getSummary(10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
        verify(dashboardService).getSummary(10);
    }

    @Test
    @DisplayName("getSummary - exception → 500")
    void testGetSummary_Exception() {
        when(dashboardService.getSummary(10)).thenThrow(new RuntimeException("err"));

        ResponseEntity<DashboardSummaryDTO> response = controller.getSummary(10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("getActivities - missing X-User-Id → 401")
    void testGetActivities_MissingUserId() {
        ResponseEntity<ActivitiesResponseDTO> response = controller.getActivities(null, 10);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(dashboardService);
    }

    @Test
    @DisplayName("getActivities - limit được clamp 1..50 và success")
    void testGetActivities_LimitClampAndSuccess() {
        ActivitiesResponseDTO dto = new ActivitiesResponseDTO();
        when(dashboardService.getRecentActivities(5, 50)).thenReturn(dto);

        // limit > 50 sẽ bị clamp xuống 50
        ResponseEntity<ActivitiesResponseDTO> response = controller.getActivities(5, 100);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
        verify(dashboardService).getRecentActivities(5, 50);
    }

    @Test
    @DisplayName("getActivities - exception → 500")
    void testGetActivities_Exception() {
        when(dashboardService.getRecentActivities(5, 10))
                .thenThrow(new RuntimeException("err"));

        ResponseEntity<ActivitiesResponseDTO> response = controller.getActivities(5, 10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("getAlerts - missing X-User-Id → 401")
    void testGetAlerts_MissingUserId() {
        ResponseEntity<AlertsResponseDTO> response = controller.getAlerts(null, 10);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(dashboardService);
    }

    @Test
    @DisplayName("getAlerts - limit clamp và success")
    void testGetAlerts_LimitClampAndSuccess() {
        AlertsResponseDTO dto = new AlertsResponseDTO();
        when(dashboardService.getAlerts(3, 1)).thenReturn(dto);

        // limit < 1 sẽ bị nâng lên 1
        ResponseEntity<AlertsResponseDTO> response = controller.getAlerts(3, 0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
        verify(dashboardService).getAlerts(3, 1);
    }

    @Test
    @DisplayName("getAlerts - exception → 500")
    void testGetAlerts_Exception() {
        when(dashboardService.getAlerts(3, 10)).thenThrow(new RuntimeException("err"));

        ResponseEntity<AlertsResponseDTO> response = controller.getAlerts(3, 10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }
}


