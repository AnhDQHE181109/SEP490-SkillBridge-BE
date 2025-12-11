package com.skillbridge.dto.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Update Project Type Request DTO
 * Request body for updating a project type
 */
public class UpdateProjectTypeRequest {

    @NotBlank(message = "Project type name is required")
    @Size(max = 128, message = "Project type name must not exceed 128 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    // Constructors
    public UpdateProjectTypeRequest() {
    }

    public UpdateProjectTypeRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

