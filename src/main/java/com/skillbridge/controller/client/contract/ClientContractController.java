package com.skillbridge.controller.client.contract;

import com.skillbridge.dto.contract.response.ContractDetailDTO;
import com.skillbridge.dto.contract.response.ContractListResponse;
import com.skillbridge.service.contract.ContractDetailService;
import com.skillbridge.service.contract.ContractListService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client Contract Controller
 * Handles contract list endpoints for client portal
 */
@RestController
@RequestMapping("/client/contracts")
@CrossOrigin(origins = "*")
public class ClientContractController {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientContractController.class);
    
    @Autowired
    private ContractListService contractListService;
    
    @Autowired
    private ContractDetailService contractDetailService;
    
    /**
     * Get contracts for authenticated client
     * GET /api/client/contracts
     * 
     * Query parameters:
     * - search: Search query (optional)
     * - status: Status filter (optional, "All" or specific status)
     * - type: Type filter (optional, "All", "MSA", or "SOW")
     * - page: Page number (default: 0)
     * - size: Page size (default: 20)
     * 
     * Note: Authentication is handled by SecurityConfig
     * The clientUserId should be extracted from JWT token or session
     * For now, we'll use a header or request parameter (should be replaced with JWT extraction)
     */
    @GetMapping
    public ResponseEntity<?> getContracts(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String type,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        try {
            logger.info("GET /client/contracts - userId: {}, search: {}, status: {}, type: {}, page: {}, size: {}", 
                userId, search, status, type, page, size);
            
            // TODO: Extract userId from JWT token when JWT authentication is fully implemented
            // For now, using header (in production, extract from authentication token)
            
            if (userId == null) {
                // Temporary: For testing, use a default user ID
                // This should be replaced with JWT token extraction
                userId = 1; // Remove this after JWT implementation
                logger.warn("No userId provided, using default userId: 1");
            }

            ContractListResponse response = contractListService.getContracts(
                userId,
                search,
                status,
                type,
                page,
                size
            );

            logger.info("Successfully retrieved {} contracts for userId: {}", response.getTotalElements(), userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching contracts for userId: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to fetch contracts: " + e.getMessage()));
        }
    }
    
    /**
     * Get contract detail for authenticated client
     * GET /api/client/contracts/{contractId}
     */
    @GetMapping("/{contractId}")
    public ResponseEntity<?> getContractDetail(
        @PathVariable Integer contractId,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        try {
            logger.info("GET /client/contracts/{} - userId: {}", contractId, userId);
            
            if (userId == null) {
                userId = 1; // Temporary default for testing
                logger.warn("No userId provided, using default userId: 1");
            }
            
            ContractDetailDTO detail = contractDetailService.getContractDetail(contractId, userId);
            logger.info("Successfully retrieved contract detail for contractId: {}, userId: {}", contractId, userId);
            return ResponseEntity.ok(detail);
            
        } catch (jakarta.persistence.EntityNotFoundException e) {
            logger.error("Contract not found: contractId={}, userId={}", contractId, userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Contract not found"));
        } catch (Exception e) {
            logger.error("Error fetching contract detail: contractId={}, userId={}", contractId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to fetch contract detail: " + e.getMessage()));
        }
    }
    
    /**
     * Get all versions of a SOW contract for client
     * GET /api/client/contracts/{contractId}/versions
     */
    @GetMapping("/{contractId}/versions")
    public ResponseEntity<?> getSOWContractVersions(
        @PathVariable Integer contractId,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        try {
            logger.info("GET /client/contracts/{}/versions - userId: {}", contractId, userId);
            
            if (userId == null) {
                userId = 1; // Temporary default for testing
                logger.warn("No userId provided, using default userId: 1");
            }
            
            List<ContractDetailDTO> versions = contractDetailService.getSOWContractVersions(contractId, userId);
            logger.info("Successfully retrieved {} versions for contractId: {}, userId: {}", versions.size(), contractId, userId);
            return ResponseEntity.ok(versions);
            
        } catch (jakarta.persistence.EntityNotFoundException e) {
            logger.error("Contract not found: contractId={}, userId={}", contractId, userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Contract not found"));
        } catch (Exception e) {
            logger.error("Error fetching contract versions: contractId={}, userId={}", contractId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to fetch contract versions: " + e.getMessage()));
        }
    }
    
    /**
     * Approve contract
     * POST /api/client/contracts/{contractId}/approve
     */
    @PostMapping("/{contractId}/approve")
    public ResponseEntity<?> approveContract(
        @PathVariable Integer contractId,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        try {
            logger.info("POST /client/contracts/{}/approve - userId: {}", contractId, userId);
            
            if (userId == null) {
                userId = 1; // Temporary default for testing
                logger.warn("No userId provided, using default userId: 1");
            }
            
            contractDetailService.approveContract(contractId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Contract approved successfully");
            response.put("contractId", contractId);
            response.put("status", "Approved");
            
            logger.info("Successfully approved contract: contractId={}, userId={}", contractId, userId);
            return ResponseEntity.ok(response);
            
        } catch (jakarta.persistence.EntityNotFoundException e) {
            logger.error("Contract not found: contractId={}, userId={}", contractId, userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Contract not found"));
        } catch (IllegalStateException e) {
            logger.error("Cannot approve contract: contractId={}, userId={}, error={}", contractId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error approving contract: contractId={}, userId={}", contractId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to approve contract: " + e.getMessage()));
        }
    }
    
    /**
     * Add comment to contract
     * POST /api/client/contracts/{contractId}/comment
     */
    @PostMapping("/{contractId}/comment")
    public ResponseEntity<?> addComment(
        @PathVariable Integer contractId,
        @RequestBody Map<String, String> request,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        try {
            logger.info("POST /client/contracts/{}/comment - userId: {}", contractId, userId);
            
            if (userId == null) {
                userId = 1; // Temporary default for testing
                logger.warn("No userId provided, using default userId: 1");
            }
            
            String comment = request.get("comment");
            if (comment == null || comment.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Comment is required"));
            }
            
            contractDetailService.addComment(contractId, userId, comment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Comment added successfully");
            response.put("contractId", contractId);
            
            logger.info("Successfully added comment to contract: contractId={}, userId={}", contractId, userId);
            return ResponseEntity.ok(response);
            
        } catch (jakarta.persistence.EntityNotFoundException e) {
            logger.error("Contract not found: contractId={}, userId={}", contractId, userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Contract not found"));
        } catch (Exception e) {
            logger.error("Error adding comment: contractId={}, userId={}", contractId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to add comment: " + e.getMessage()));
        }
    }
    
    /**
     * Cancel contract
     * POST /api/client/contracts/{contractId}/cancel
     */
    @PostMapping("/{contractId}/cancel")
    public ResponseEntity<?> cancelContract(
        @PathVariable Integer contractId,
        @RequestBody Map<String, String> request,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        try {
            logger.info("POST /client/contracts/{}/cancel - userId: {}", contractId, userId);
            
            if (userId == null) {
                userId = 1; // Temporary default for testing
                logger.warn("No userId provided, using default userId: 1");
            }
            
            String reason = request.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Reason is required"));
            }
            
            contractDetailService.cancelContract(contractId, userId, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Contract cancelled successfully");
            response.put("contractId", contractId);
            response.put("status", "Terminated");
            
            logger.info("Successfully cancelled contract: contractId={}, userId={}", contractId, userId);
            return ResponseEntity.ok(response);
            
        } catch (jakarta.persistence.EntityNotFoundException e) {
            logger.error("Contract not found: contractId={}, userId={}", contractId, userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Contract not found"));
        } catch (IllegalStateException e) {
            logger.error("Cannot cancel contract: contractId={}, userId={}, error={}", contractId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error cancelling contract: contractId={}, userId={}", contractId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to cancel contract: " + e.getMessage()));
        }
    }
    
    /**
     * Error response DTO
     * Used by Spring for JSON serialization
     */
    private static class ErrorResponse {
        private final String message;
        
        public ErrorResponse(String message) {
            this.message = message;
        }
        
        @SuppressWarnings("unused") // Used by Spring for JSON serialization
        public String getMessage() {
            return message;
        }
    }
}

