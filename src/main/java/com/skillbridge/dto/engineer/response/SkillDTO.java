package com.skillbridge.dto.engineer.response;

/**
 * Skill DTO
 * Represents a skill with its details
 */
public class SkillDTO {
    private Integer id;
    private String name;
    private String level;
    private Integer yearsOfExperience;

    // Constructors
    public SkillDTO() {
    }

    public SkillDTO(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public SkillDTO(Integer id, String name, String level, Integer yearsOfExperience) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.yearsOfExperience = yearsOfExperience;
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

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    @Override
    public String toString() {
        return "SkillDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", level='" + level + '\'' +
                ", yearsOfExperience=" + yearsOfExperience +
                '}';
    }
}

