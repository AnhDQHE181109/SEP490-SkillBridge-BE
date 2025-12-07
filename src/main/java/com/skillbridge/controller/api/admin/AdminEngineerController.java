package com.skillbridge.controller.api.admin;

import com.skillbridge.dto.admin.request.CreateEngineerRequest;
import com.skillbridge.dto.admin.request.UpdateEngineerRequest;
import com.skillbridge.dto.admin.response.EngineerListResponse;
import com.skillbridge.dto.admin.response.EngineerResponseDTO;
import com.skillbridge.service.admin.AdminEngineerService;
import com.skillbridge.service.common.S3Service;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin Engineer Controller
 * Handles engineer management endpoints for Admin Engineer List
 * Note: context-path is /api, so full path will be /api/admin/engineers
 */
@RestController
@RequestMapping("/admin/engineers")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:4200"}, 
             allowCredentials = "true",
             maxAge = 3600)
public class AdminEngineerController {

    @Autowired
    private AdminEngineerService adminEngineerService;

    @Autowired(required = false)
    private S3Service s3Service;

    /**
     * Get all engineers with pagination and search
     * GET /api/admin/engineers
     * 
     * Query parameters:
     * - page: Page number (default: 0)
     * - size: Page size (default: 20)
     * - search: Search term (optional)
     */
    @GetMapping
    public ResponseEntity<?> getAllEngineers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        try {
            EngineerListResponse response = adminEngineerService.getAllEngineers(page, size, search);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to get engineers: " + e.getMessage()));
        }
    }

    /**
     * Get engineer by ID
     * GET /api/admin/engineers/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getEngineer(@PathVariable Integer id) {
        try {
            EngineerResponseDTO response = adminEngineerService.getEngineerById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to get engineer: " + e.getMessage()));
        }
    }

    /**
     * Get engineer detail by ID with certificates and skills
     * GET /api/admin/engineers/{id}/detail
     */
    @GetMapping("/{id}/detail")
    public ResponseEntity<?> getEngineerDetail(@PathVariable Integer id) {
        try {
            com.skillbridge.dto.admin.response.EngineerDetailResponseDTO response = adminEngineerService.getEngineerDetailById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to get engineer detail: " + e.getMessage()));
        }
    }

    /**
     * Create a new engineer
     * POST /api/admin/engineers
     */
    @PostMapping
    public ResponseEntity<?> createEngineer(@Valid @RequestBody CreateEngineerRequest request) {
        try {
            EngineerResponseDTO response = adminEngineerService.createEngineer(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to create engineer: " + e.getMessage()));
        }
    }

    /**
     * Update an engineer
     * PUT /api/admin/engineers/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEngineer(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateEngineerRequest request
    ) {
        try {
            EngineerResponseDTO response = adminEngineerService.updateEngineer(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to update engineer: " + e.getMessage()));
        }
    }

    /**
     * Delete an engineer
     * DELETE /api/admin/engineers/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEngineer(@PathVariable Integer id) {
        try {
            adminEngineerService.deleteEngineer(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to delete engineer: " + e.getMessage()));
        }
    }

    /**
     * Upload profile image for engineer
     * POST /api/admin/engineers/upload-image
     */
    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("File is empty"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("File must be an image"));
            }

            // Validate file size (5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Image size must be less than 5MB"));
            }

            if (s3Service == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponse("S3 service is not configured"));
            }

            // Upload to S3 (returns S3 key)
            String s3Key = s3Service.uploadPublicFile(file, "engineers/profiles");
            
            // Generate presigned URL with 24 hours expiration
            String presignedUrl = s3Service.getPresignedUrl(s3Key, 24 * 60); // 24 hours in minutes

            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", presignedUrl);
            response.put("s3Key", s3Key); // Also return S3 key for storage
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to upload image: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        String errorMessage = "Validation failed: " + String.join(", ", errors.values());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(errorMessage));
    }

    /**
     * Error response class
     */
    private static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}

