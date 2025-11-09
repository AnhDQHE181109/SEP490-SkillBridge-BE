package com.skillbridge.dto.contact.response;

/**
 * DTO for create contact response
 */
public class CreateContactResponse {
    private boolean success;
    private String message;
    private Integer contactId;

    public CreateContactResponse() {
    }

    public CreateContactResponse(boolean success, String message, Integer contactId) {
        this.success = success;
        this.message = message;
        this.contactId = contactId;
    }

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

