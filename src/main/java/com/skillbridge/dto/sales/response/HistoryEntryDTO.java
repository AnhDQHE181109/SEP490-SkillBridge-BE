package com.skillbridge.dto.sales.response;

import java.time.LocalDateTime;

/**
 * DTO for History Entry
 */
public class HistoryEntryDTO {
    private Integer id;
    private String date; // Format: "YYYY/MM/DD"
    private String activity; // e.g., "Proposal Draft v2 uploaded by Sale 01"
    private String fileLink; // e.g., "Proposal_v2.pdf"
    private String fileUrl; // S3 URL
    private LocalDateTime createdAt;

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getFileLink() {
        return fileLink;
    }

    public void setFileLink(String fileLink) {
        this.fileLink = fileLink;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

