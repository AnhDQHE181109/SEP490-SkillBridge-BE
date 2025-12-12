package com.skillbridge.dto.contract.response;

import java.time.LocalDateTime;

/**
 * Project Close Request Detail DTO
 * Detailed information about a project close request
 * Story-41: Project Close Request for SOW Contract
 */
public class ProjectCloseRequestDetailDTO {
    private Integer id;
    private Integer sowId;
    private String sowContractName; // SOW contract name
    private String sowPeriod; // SOW period (e.g., "2025/12/1-2025/12/31")
    private String status; // "Pending", "ClientApproved", "Rejected"
    private String message;
    private String links; // URLs separated by newlines
    private UserInfoDTO requestedBy; // User who created the request
    private String clientRejectReason; // Reason if rejected
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String sowStatus; // SOW status (e.g., "Active", "Completed")

    // Nested DTO for user information
    public static class UserInfoDTO {
        private Integer id;
        private String name;
        private String email;

        public UserInfoDTO() {
        }

        public UserInfoDTO(Integer id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    // Constructors
    public ProjectCloseRequestDetailDTO() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSowId() {
        return sowId;
    }

    public void setSowId(Integer sowId) {
        this.sowId = sowId;
    }

    public String getSowContractName() {
        return sowContractName;
    }

    public void setSowContractName(String sowContractName) {
        this.sowContractName = sowContractName;
    }

    public String getSowPeriod() {
        return sowPeriod;
    }

    public void setSowPeriod(String sowPeriod) {
        this.sowPeriod = sowPeriod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

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

    public UserInfoDTO getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(UserInfoDTO requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getClientRejectReason() {
        return clientRejectReason;
    }

    public void setClientRejectReason(String clientRejectReason) {
        this.clientRejectReason = clientRejectReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getSowStatus() {
        return sowStatus;
    }

    public void setSowStatus(String sowStatus) {
        this.sowStatus = sowStatus;
    }
}

