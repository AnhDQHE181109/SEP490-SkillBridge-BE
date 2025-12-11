package com.skillbridge.validator;

import com.skillbridge.dto.sales.request.CreateOpportunityRequest;

import java.math.BigDecimal;

/**
 * Create Opportunity Validator
 * Validates create opportunity request data according to business rules
 */
public class CreateOpportunityValidator {

    private static final String EMAIL_PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private static final String CURRENCY_PATTERN = "^[A-Z]{3}$"; // ISO 4217 currency code format (3 uppercase letters)

    /**
     * Validate create opportunity request data
     * @param request Create opportunity request data
     * @return ValidationResult with validation status and message
     */
    public ValidationResult validate(CreateOpportunityRequest request) {
        // Validate client name
        if (request.getClientName() == null || request.getClientName().trim().isEmpty()) {
            return ValidationResult.failure("Client name is required");
        }
        if (request.getClientName().trim().length() < 2) {
            return ValidationResult.failure("Client name must be at least 2 characters");
        }
        if (request.getClientName().trim().length() > 255) {
            return ValidationResult.failure("Client name must be less than 255 characters");
        }

        // Validate client company (optional field)
        if (request.getClientCompany() != null && !request.getClientCompany().trim().isEmpty()) {
            if (request.getClientCompany().trim().length() < 2) {
                return ValidationResult.failure("Client company must be at least 2 characters");
            }
            if (request.getClientCompany().trim().length() > 255) {
                return ValidationResult.failure("Client company must be less than 255 characters");
            }
        }

        // Validate client email
        if (request.getClientEmail() == null || request.getClientEmail().trim().isEmpty()) {
            return ValidationResult.failure("Client email is required");
        }
        if (!request.getClientEmail().trim().matches(EMAIL_PATTERN)) {
            return ValidationResult.failure("Client email must be valid");
        }
        if (request.getClientEmail().trim().length() > 255) {
            return ValidationResult.failure("Client email must be less than 255 characters");
        }

        // Validate probability (optional field, range 0-100)
        if (request.getProbability() != null) {
            if (request.getProbability() < 0 || request.getProbability() > 100) {
                return ValidationResult.failure("Probability must be between 0 and 100");
            }
        }

        // Validate estimated value (optional field, must be >= 0)
        if (request.getEstValue() != null) {
            if (request.getEstValue().compareTo(BigDecimal.ZERO) < 0) {
                return ValidationResult.failure("Estimated value must be greater than or equal to 0");
            }
        }

        // Validate currency (optional field)
        if (request.getCurrency() != null && !request.getCurrency().trim().isEmpty()) {
            if (request.getCurrency().trim().length() > 10) {
                return ValidationResult.failure("Currency code must be less than 10 characters");
            }
            // Validate currency code format (3 uppercase letters)
            if (!request.getCurrency().trim().matches(CURRENCY_PATTERN)) {
                return ValidationResult.failure("Currency code must be 3 uppercase letters (e.g., JPY, USD, EUR)");
            }
        }

        return ValidationResult.success();
    }
}

