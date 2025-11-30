package com.skillbridge.dto.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SubSkillRequest {
    @NotBlank(message = "Sub-skill name is required")
    @Size(max = 128, message = "Sub-skill name must not exceed 128 characters")
    private String name;

    // Constructors
    public SubSkillRequest() {
    }

    public SubSkillRequest(String name) {
        this.name = name;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
