package com.skillbridge.validator;

import com.skillbridge.dto.sales.request.CreateMSARequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Create MSA Validator
 * Validates create MSA request data according to business rules
 */
public class CreateMSAValidator {

    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    private static final String CURRENCY_PATTERN = "^[A-Z]{3}$"; // ISO 4217 currency code format (3 uppercase letters)

    /**
     * Validate create MSA request data
     * @param request Create MSA request data
     * @return ValidationResult with validation status and message
     */
    public ValidationResult validate(CreateMSARequest request) {
        // Validate client ID
        if (request.getClientId() == null) {
            return ValidationResult.failure("Client ID is required");
        }

        // Validate assignee user ID
        if (request.getAssigneeUserId() == null) {
            return ValidationResult.failure("Assignee user ID is required");
        }

        // Validate client contact ID
        if (request.getClientContactId() == null) {
            return ValidationResult.failure("Client contact ID is required");
        }

        // Validate landbridge contact ID
        if (request.getLandbridgeContactId() == null) {
            return ValidationResult.failure("LandBridge contact ID is required");
        }

        // Validate effective start date
        if (request.getEffectiveStart() == null || request.getEffectiveStart().trim().isEmpty()) {
            return ValidationResult.failure("Effective start date is required");
        }
        
        LocalDate effectiveStart;
        try {
            effectiveStart = LocalDate.parse(request.getEffectiveStart().trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return ValidationResult.failure("Effective start date must be in format YYYY-MM-DD");
        }

        // Validate effective end date
        if (request.getEffectiveEnd() == null || request.getEffectiveEnd().trim().isEmpty()) {
            return ValidationResult.failure("Effective end date is required");
        }
        
        LocalDate effectiveEnd;
        try {
            effectiveEnd = LocalDate.parse(request.getEffectiveEnd().trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return ValidationResult.failure("Effective end date must be in format YYYY-MM-DD");
        }

        // Validate end date is after start date
        if (!effectiveEnd.isAfter(effectiveStart)) {
            return ValidationResult.failure("Effective end date must be after effective start date");
        }

        // Validate currency (optional field)
        if (request.getCurrency() != null && !request.getCurrency().trim().isEmpty()) {
            if (request.getCurrency().trim().length() > 16) {
                return ValidationResult.failure("Currency code must be less than 16 characters");
            }
            // Validate currency code format (3 uppercase letters) if provided
            if (request.getCurrency().trim().matches(CURRENCY_PATTERN)) {
                // Valid format
            } else if (request.getCurrency().trim().length() == 3) {
                // Only validate format if it's exactly 3 characters
                return ValidationResult.failure("Currency code must be 3 uppercase letters (e.g., JPY, USD, EUR)");
            }
        }

        // Validate payment terms length (optional field)
        if (request.getPaymentTerms() != null && request.getPaymentTerms().trim().length() > 128) {
            return ValidationResult.failure("Payment terms must be less than 128 characters");
        }

        // Validate invoicing cycle length (optional field)
        if (request.getInvoicingCycle() != null && request.getInvoicingCycle().trim().length() > 64) {
            return ValidationResult.failure("Invoicing cycle must be less than 64 characters");
        }

        // Validate billing day length (optional field)
        if (request.getBillingDay() != null && request.getBillingDay().trim().length() > 64) {
            return ValidationResult.failure("Billing day must be less than 64 characters");
        }

        // Validate tax withholding length (optional field)
        if (request.getTaxWithholding() != null && request.getTaxWithholding().trim().length() > 16) {
            return ValidationResult.failure("Tax withholding must be less than 16 characters");
        }

        // Validate IP ownership length (optional field)
        if (request.getIpOwnership() != null && request.getIpOwnership().trim().length() > 128) {
            return ValidationResult.failure("IP ownership must be less than 128 characters");
        }

        // Validate governing law length (optional field)
        if (request.getGoverningLaw() != null && request.getGoverningLaw().trim().length() > 64) {
            return ValidationResult.failure("Governing law must be less than 64 characters");
        }

        // Validate note length (optional field)
        if (request.getNote() != null && request.getNote().trim().length() > 1000) {
            return ValidationResult.failure("Note must be less than 1000 characters");
        }

        // Validate review notes length (optional field)
        if (request.getReviewNotes() != null && request.getReviewNotes().trim().length() > 1000) {
            return ValidationResult.failure("Review notes must be less than 1000 characters");
        }

        return ValidationResult.success();
    }
}

