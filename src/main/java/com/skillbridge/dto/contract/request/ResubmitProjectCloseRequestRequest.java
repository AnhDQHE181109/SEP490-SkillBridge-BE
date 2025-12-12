package com.skillbridge.dto.contract.request;

/**
 * Resubmit Project Close Request Request DTO
 * Data transfer object for resubmitting a rejected project close request
 * Story-41: Project Close Request for SOW Contract
 */
public class ResubmitProjectCloseRequestRequest {
    private String message; // Optional (max 5000 characters)
    private String links; // Optional (max 2000 characters) - URLs separated by newlines

    // Constructors
    public ResubmitProjectCloseRequestRequest() {
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLinks() {
        return links;
    }

    public void setLinks(String links) {
        this.links = links;
    }
}

