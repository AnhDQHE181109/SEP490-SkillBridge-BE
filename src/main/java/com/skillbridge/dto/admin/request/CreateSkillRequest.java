package com.skillbridge.dto.admin.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Create Skill Request DTO
 * Request body for creating a new skill with optional sub-skills
 */
public class CreateSkillRequest {

    @NotBlank(message = "Skill name is required")
    @Size(max = 128, message = "Skill name must not exceed 128 characters")
    private String name;

    private String description;

    @Valid
    private List<SubSkillRequest> subSkills;

    // Constructors
    public CreateSkillRequest() {
    }

    public CreateSkillRequest(String name, String description, List<SubSkillRequest> subSkills) {
        this.name = name;
        this.description = description;
        this.subSkills = subSkills;
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

    public List<SubSkillRequest> getSubSkills() {
        return subSkills;
    }

    public void setSubSkills(List<SubSkillRequest> subSkills) {
        this.subSkills = subSkills;
    }

}

