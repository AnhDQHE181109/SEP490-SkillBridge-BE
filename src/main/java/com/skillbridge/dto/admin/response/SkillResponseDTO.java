package com.skillbridge.dto.admin.response;

/**
 * Skill Response DTO
 * Response DTO for skill information in Admin Master Data
 */
public class SkillResponseDTO {
    private Integer id;
    private String name;
    private String description;
    private Integer parentSkillId;

    // Constructors
    public SkillResponseDTO() {
    }

    public SkillResponseDTO(Integer id, String name, String description, Integer parentSkillId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.parentSkillId = parentSkillId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getParentSkillId() {
        return parentSkillId;
    }

    public void setParentSkillId(Integer parentSkillId) {
        this.parentSkillId = parentSkillId;
    }
}

