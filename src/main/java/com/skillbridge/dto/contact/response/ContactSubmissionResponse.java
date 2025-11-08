package com.skillbridge.dto.contact.response;

/**
 * Contact Submission Response DTO
 * Response object for contact form submission
 */
public class ContactSubmissionResponse {

    private boolean success;
    private String message;
    private Integer contactId;

    // Constructors
    public ContactSubmissionResponse() {
    }

    public ContactSubmissionResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ContactSubmissionResponse(boolean success, String message, Integer contactId) {
        this.success = success;
        this.message = message;
        this.contactId = contactId;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getContactId() {
        return contactId;
    }

    public void setContactId(Integer contactId) {
        this.contactId = contactId;
    }
}

