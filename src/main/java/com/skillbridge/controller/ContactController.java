package com.skillbridge.controller;

import com.skillbridge.dto.ContactFormData;
import com.skillbridge.dto.ContactSubmissionResponse;
import com.skillbridge.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Contact Controller
 * REST API endpoints for contact form submissions
 */
@RestController
@RequestMapping("/public/contact")
@CrossOrigin(origins = "*")
public class ContactController {

    @Autowired
    private ContactService contactService;

    /**
     * Submit contact form
     * POST /api/public/contact/submit
     * 
     * @param contactData Contact form data
     * @return ContactSubmissionResponse
     */
    @PostMapping("/submit")
    public ResponseEntity<ContactSubmissionResponse> submitContact(
            @Valid @RequestBody ContactFormData contactData
    ) {
        try {
            ContactSubmissionResponse response = contactService.processContactSubmission(contactData);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Validation errors or business logic errors
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ContactSubmissionResponse(false, e.getMessage()));
        } catch (Exception e) {
            // Log the full exception for debugging
            System.err.println("Error processing contact submission: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ContactSubmissionResponse(false, "Failed to process contact submission. Please try again later."));
        }
    }

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ContactSubmissionResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        String errorMessage = "Validation failed: " + String.join(", ", errors.values());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ContactSubmissionResponse(false, errorMessage));
    }
}

