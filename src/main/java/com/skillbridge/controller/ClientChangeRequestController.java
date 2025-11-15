package com.skillbridge.controller;

import com.skillbridge.dto.contract.request.CreateChangeRequestRequest;
import com.skillbridge.dto.contract.response.ChangeRequestResponse;
import com.skillbridge.service.contract.ChangeRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Client Change Request Controller
 * Handles HTTP requests for change request operations
 */
@RestController
@RequestMapping("/client/contracts/{contractId}/change-requests")
@CrossOrigin(origins = "*")
public class ClientChangeRequestController {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientChangeRequestController.class);
    
    @Autowired
    private ChangeRequestService changeRequestService;
    
    /**
     * Create a new change request
     */
    @PostMapping
    public ResponseEntity<?> createChangeRequest(
        @PathVariable Integer contractId,
        @RequestParam("title") String title,
        @RequestParam("type") String type,
        @RequestParam("description") String description,
        @RequestParam("reason") String reason,
        @RequestParam("desiredStartDate") @DateTimeFormat(pattern = "yyyy/MM/dd") LocalDate desiredStartDate,
        @RequestParam("desiredEndDate") @DateTimeFormat(pattern = "yyyy/MM/dd") LocalDate desiredEndDate,
        @RequestParam("expectedExtraCost") BigDecimal expectedExtraCost,
        @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments,
        @RequestHeader(value = "Authorization", required = false) String token,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User ID is required");
            }
            
            // Create request DTO
            CreateChangeRequestRequest request = new CreateChangeRequestRequest();
            request.setTitle(title);
            request.setType(type);
            request.setDescription(description);
            request.setReason(reason);
            request.setDesiredStartDate(desiredStartDate);
            request.setDesiredEndDate(desiredEndDate);
            request.setExpectedExtraCost(expectedExtraCost);
            
            // Create change request
            ChangeRequestResponse response = changeRequestService.createChangeRequest(
                contractId,
                userId,
                request,
                attachments
            );
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error creating change request", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Validation error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating change request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to create change request: " + e.getMessage());
        }
    }
    
    /**
     * Save change request as draft
     */
    @PostMapping("/draft")
    public ResponseEntity<?> saveChangeRequestDraft(
        @PathVariable Integer contractId,
        @RequestParam(value = "title", required = false) String title,
        @RequestParam(value = "type", required = false) String type,
        @RequestParam(value = "description", required = false) String description,
        @RequestParam(value = "reason", required = false) String reason,
        @RequestParam(value = "desiredStartDate", required = false) @DateTimeFormat(pattern = "yyyy/MM/dd") LocalDate desiredStartDate,
        @RequestParam(value = "desiredEndDate", required = false) @DateTimeFormat(pattern = "yyyy/MM/dd") LocalDate desiredEndDate,
        @RequestParam(value = "expectedExtraCost", required = false) BigDecimal expectedExtraCost,
        @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments,
        @RequestHeader(value = "Authorization", required = false) String token,
        @RequestHeader(value = "X-User-Id", required = false) Integer userId
    ) {
        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User ID is required");
            }
            
            // Create request DTO (all fields optional for draft)
            CreateChangeRequestRequest request = new CreateChangeRequestRequest();
            request.setTitle(title);
            request.setType(type);
            request.setDescription(description);
            request.setReason(reason);
            request.setDesiredStartDate(desiredStartDate);
            request.setDesiredEndDate(desiredEndDate);
            request.setExpectedExtraCost(expectedExtraCost);
            
            // Save as draft
            ChangeRequestResponse response = changeRequestService.saveChangeRequestDraft(
                contractId,
                userId,
                request,
                attachments
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error saving change request draft", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to save change request draft: " + e.getMessage());
        }
    }
}

