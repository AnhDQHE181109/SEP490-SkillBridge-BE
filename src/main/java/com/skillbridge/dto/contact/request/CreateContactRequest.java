package com.skillbridge.dto.contact.request;

/**
 * DTO for creating a new contact
 */
public class CreateContactRequest {
    private String title;
    private String description;

    public CreateContactRequest() {
    }

    public CreateContactRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

