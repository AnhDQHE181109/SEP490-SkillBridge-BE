package com.skillbridge.controller;

import com.skillbridge.dto.contract.request.UpdateChangeRequestRequest;
import com.skillbridge.dto.contract.response.ChangeRequestDetailDTO;
import com.skillbridge.service.contract.ChangeRequestDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Client Change Request Detail Controller
 * Handles API endpoints for client change request detail operations
 */
@RestController
@RequestMapping("/client/contracts/{contractId}/change-requests/{changeRequestId}")
@CrossOrigin(origins = "*")
public class ClientChangeRequestDetailController {
    
    @Autowired
    private ChangeRequestDetailService changeRequestDetailService;
    
    /**
     * Get change request detail
     */
    @GetMapping
    public ResponseEntity<?> getChangeRequestDetail(
        @PathVariable Integer contractId,
        @PathVariable Integer changeRequestId,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User ID is required"));
            }
            
            ChangeRequestDetailDTO detail = changeRequestDetailService.getChangeRequestDetail(
                contractId, changeRequestId, userId
            );
            
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Update change request (Draft status only)
     */
    @PutMapping
    public ResponseEntity<?> updateChangeRequest(
        @PathVariable Integer contractId,
        @PathVariable Integer changeRequestId,
        @RequestBody UpdateChangeRequestRequest request,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User ID is required"));
            }
            
            changeRequestDetailService.updateChangeRequest(
                contractId, changeRequestId, userId, request
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Change request updated successfully");
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Submit change request (Draft -> Under Review)
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitChangeRequest(
        @PathVariable Integer contractId,
        @PathVariable Integer changeRequestId,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User ID is required"));
            }
            
            changeRequestDetailService.submitChangeRequest(
                contractId, changeRequestId, userId
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Change request submitted successfully");
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Approve change request (Under Review -> Active)
     */
    @PostMapping("/approve")
    public ResponseEntity<?> approveChangeRequest(
        @PathVariable Integer contractId,
        @PathVariable Integer changeRequestId,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User ID is required"));
            }
            
            changeRequestDetailService.approveChangeRequest(
                contractId, changeRequestId, userId
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Change request approved successfully");
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Request for change (Under Review -> Request for Change)
     */
    @PostMapping("/request-for-change")
    public ResponseEntity<?> requestForChange(
        @PathVariable Integer contractId,
        @PathVariable Integer changeRequestId,
        @RequestBody(required = false) Map<String, String> requestBody,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User ID is required"));
            }
            
            String message = requestBody != null ? requestBody.get("message") : null;
            
            changeRequestDetailService.requestForChange(
                contractId, changeRequestId, userId, message
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Change request status updated to Request for Change");
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Terminate change request
     */
    @PostMapping("/terminate")
    public ResponseEntity<?> terminateChangeRequest(
        @PathVariable Integer contractId,
        @PathVariable Integer changeRequestId,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User ID is required"));
            }
            
            changeRequestDetailService.terminateChangeRequest(
                contractId, changeRequestId, userId
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Change request terminated successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

