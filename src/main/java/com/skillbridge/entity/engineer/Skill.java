package com.skillbridge.entity.engineer;

import jakarta.persistence.*;

/**
 * Skill Entity
 * Represents a skill in the system
 */
@Entity
@Table(name = "skills")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 128)
    private String name;

    @Column(name = "parent_skill_id")
    private Integer parentSkillId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Constructors
    public Skill() {
    }

    public Skill(String name) {
        this.name = name;
    }

    public Skill(String name, Integer parentSkillId) {
        this.name = name;
        this.parentSkillId = parentSkillId;
    }

    public Skill(String name, Integer parentSkillId, String description) {
        this.name = name;
        this.parentSkillId = parentSkillId;
        this.description = description;
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

    public Integer getParentSkillId() {
        return parentSkillId;
    }

    public void setParentSkillId(Integer parentSkillId) {
        this.parentSkillId = parentSkillId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Skill{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", parentSkillId=" + parentSkillId +
                ", description='" + description + '\'' +
                '}';
    }
}

