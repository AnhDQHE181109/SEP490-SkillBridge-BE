package com.skillbridge.entity.engineer;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Primary Key for EngineerSkill
 */
public class EngineerSkillId implements Serializable {
    private Integer engineerId;
    private Integer skillId;

    public EngineerSkillId() {
    }

    public EngineerSkillId(Integer engineerId, Integer skillId) {
        this.engineerId = engineerId;
        this.skillId = skillId;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EngineerSkillId that = (EngineerSkillId) o;
        return Objects.equals(engineerId, that.engineerId) &&
                Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(engineerId, skillId);
    }
}

