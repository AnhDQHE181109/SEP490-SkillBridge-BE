package com.skillbridge.controller.client.dashboard;

import com.skillbridge.dto.dashboard.response.*;
import com.skillbridge.service.dashboard.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Client Dashboard Controller
 * Handles dashboard endpoints for client portal
 */
@RestController
@RequestMapping("/client/dashboard")
@CrossOrigin(origins = "*")
public class ClientDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(ClientDashboardController.class);

    @Autowired
    private DashboardService dashboardService;

    /**
     * Get dashboard summary statistics
     * GET /api/client/dashboard/summary
     * 
     * Headers:
     * - Authorization: Bearer {token}
     * - X-User-Id: {userId} (for now, should be replaced with JWT extraction)
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary(
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        try {
            if (userId == null) {
                logger.warn("X-User-Id header is missing");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            DashboardSummaryDTO summary = dashboardService.getSummary(userId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error getting dashboard summary for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get recent activities
     * GET /api/client/dashboard/activities
     * 
     * Query parameters:
     * - limit: Maximum number of activities to return (default: 10)
     * 
     * Headers:
     * - Authorization: Bearer {token}
     * - X-User-Id: {userId} (for now, should be replaced with JWT extraction)
     */
    @GetMapping("/activities")
    public ResponseEntity<ActivitiesResponseDTO> getActivities(
        @RequestHeader(value = "X-User-Id", required = false) Integer userId,
        @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        try {
            if (userId == null) {
                logger.warn("X-User-Id header is missing");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Limit to reasonable range
            if (limit < 1) limit = 1;
            if (limit > 50) limit = 50;

            ActivitiesResponseDTO activities = dashboardService.getRecentActivities(userId, limit);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            logger.error("Error getting dashboard activities for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get alerts/notifications
     * GET /api/client/dashboard/alerts
     * 
     * Query parameters:
     * - limit: Maximum number of alerts to return (default: 10)
     * 
     * Headers:
     * - Authorization: Bearer {token}
     * - X-User-Id: {userId} (for now, should be replaced with JWT extraction)
     */
    @GetMapping("/alerts")
    public ResponseEntity<AlertsResponseDTO> getAlerts(
        @RequestHeader(value = "X-User-Id", required = false) Integer userId,
        @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        try {
            if (userId == null) {
                logger.warn("X-User-Id header is missing");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Limit to reasonable range
            if (limit < 1) limit = 1;
            if (limit > 50) limit = 50;

            AlertsResponseDTO alerts = dashboardService.getAlerts(userId, limit);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            logger.error("Error getting dashboard alerts for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

