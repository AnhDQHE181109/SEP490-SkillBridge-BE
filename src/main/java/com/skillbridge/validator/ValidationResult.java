package com.skillbridge.validator;

/**
 * Validation Result
 * Represents the result of form validation
 */
public class ValidationResult {
    private boolean valid;
    private String message;

    public ValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static ValidationResult success() {
        return new ValidationResult(true, "Validation passed");
    }

    public static ValidationResult failure(String message) {
        return new ValidationResult(false, message);
    }
}

