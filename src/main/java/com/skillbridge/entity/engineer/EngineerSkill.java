package com.skillbridge.entity.engineer;

import jakarta.persistence.*;

/**
 * EngineerSkill Entity
 * Represents the relationship between engineers and their skills
 * Uses composite primary key (engineer_id, skill_id)
 */
@Entity
@Table(name = "engineer_skills")
@IdClass(EngineerSkillId.class)
public class EngineerSkill {

    @Id
    @Column(name = "engineer_id", nullable = false)
    private Integer engineerId;

    @Id
    @Column(name = "skill_id", nullable = false)
    private Integer skillId;

    @Column(length = 32)
    private String level;

    @Column(name = "years")
    private Integer years;

    // Constructors
    public EngineerSkill() {
    }

    public EngineerSkill(Integer engineerId, Integer skillId) {
        this.engineerId = engineerId;
        this.skillId = skillId;
    }

    public EngineerSkill(Integer engineerId, Integer skillId, String level, Integer years) {
        this.engineerId = engineerId;
        this.skillId = skillId;
        this.level = level;
        this.years = years;
    }

    // Getters and Setters
    public Integer getEngineerId() {
        return engineerId;
    }

    public void setEngineerId(Integer engineerId) {
        this.engineerId = engineerId;
    }

    public Integer getSkillId() {
        return skillId;
    }

    public void setSkillId(Integer skillId) {
        this.skillId = skillId;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getYears() {
        return years;
    }

    public void setYears(Integer years) {
        this.years = years;
    }

    @Override
    public String toString() {
        return "EngineerSkill{" +
                "engineerId=" + engineerId +
                ", skillId=" + skillId +
                ", level='" + level + '\'' +
                ", years=" + years +
                '}';
    }
}

