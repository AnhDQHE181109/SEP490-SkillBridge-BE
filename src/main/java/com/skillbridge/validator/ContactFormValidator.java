package com.skillbridge.validator;

import com.skillbridge.dto.contact.request.ContactFormData;

/**
 * Contact Form Validator
 * Validates contact form data according to business rules
 */
public class ContactFormValidator {

    private static final String PHONE_PATTERN = "^[\\d\\-\\+\\(\\)\\s]+$";
    private static final String EMAIL_PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    /**
     * Validate contact form data
     * @param form Contact form data
     * @return ValidationResult with validation status and message
     */
    public ValidationResult validate(ContactFormData form) {
        // Validate name
        if (form.getName() == null || form.getName().trim().isEmpty()) {
            return ValidationResult.failure("Name is required");
        }
        if (form.getName().trim().length() < 2) {
            return ValidationResult.failure("Name must be at least 2 characters");
        }
        if (form.getName().trim().length() > 100) {
            return ValidationResult.failure("Name must be less than 100 characters");
        }

        // Validate company name
        if (form.getCompanyName() == null || form.getCompanyName().trim().isEmpty()) {
            return ValidationResult.failure("Company name is required");
        }
        if (form.getCompanyName().trim().length() < 2) {
            return ValidationResult.failure("Company name must be at least 2 characters");
        }
        if (form.getCompanyName().trim().length() > 100) {
            return ValidationResult.failure("Company name must be less than 100 characters");
        }

        // Validate phone
        if (form.getPhone() == null || form.getPhone().trim().isEmpty()) {
            return ValidationResult.failure("Phone is required");
        }
        if (!form.getPhone().trim().matches(PHONE_PATTERN)) {
            return ValidationResult.failure("Phone number format is invalid");
        }

        // Validate email
        if (form.getEmail() == null || form.getEmail().trim().isEmpty()) {
            return ValidationResult.failure("Email is required");
        }
        if (!form.getEmail().trim().matches(EMAIL_PATTERN)) {
            return ValidationResult.failure("Email must be valid");
        }

        // Validate title
        if (form.getTitle() == null || form.getTitle().trim().isEmpty()) {
            return ValidationResult.failure("Title is required");
        }
        if (form.getTitle().trim().length() < 2) {
            return ValidationResult.failure("Title must be at least 2 characters");
        }
        if (form.getTitle().trim().length() > 255) {
            return ValidationResult.failure("Title must be less than 255 characters");
        }

        // Validate message
        if (form.getMessage() == null || form.getMessage().trim().isEmpty()) {
            return ValidationResult.failure("Message is required");
        }
        if (form.getMessage().trim().length() < 10) {
            return ValidationResult.failure("Message must be at least 10 characters");
        }
        if (form.getMessage().trim().length() > 1000) {
            return ValidationResult.failure("Message must be less than 1000 characters");
        }

        return ValidationResult.success();
    }
}

