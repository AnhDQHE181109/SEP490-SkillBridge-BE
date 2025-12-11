package com.skillbridge.dto.admin.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Update Skill Request DTO
 * Request body for updating a skill with optional sub-skills
 */
public class UpdateSkillRequest {

    @NotBlank(message = "Skill name is required")
    @Size(max = 128, message = "Skill name must not exceed 128 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Valid
    private List<SubSkillUpdateRequest> subSkills;

    // Constructors
    public UpdateSkillRequest() {
    }

    public UpdateSkillRequest(String name, String description, List<SubSkillUpdateRequest> subSkills) {
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

    public List<SubSkillUpdateRequest> getSubSkills() {
        return subSkills;
    }

    public void setSubSkills(List<SubSkillUpdateRequest> subSkills) {
        this.subSkills = subSkills;
    }

    /**
     * Sub Skill Update Request nested class
     * Includes id for existing sub-skills, null for new ones
     */
    public static class SubSkillUpdateRequest {
        private Integer id; // null for new sub-skills
        @NotBlank(message = "Sub-skill name is required")
        @Size(max = 128, message = "Sub-skill name must not exceed 128 characters")
        private String name;

        // Constructors
        public SubSkillUpdateRequest() {
        }

        public SubSkillUpdateRequest(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        // Getters and Setters
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
    }
}

