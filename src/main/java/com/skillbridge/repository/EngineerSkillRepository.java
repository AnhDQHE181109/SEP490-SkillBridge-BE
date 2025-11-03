package com.skillbridge.repository;

import com.skillbridge.dto.SkillDTO;
import com.skillbridge.entity.EngineerSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * EngineerSkill Repository
 * Handles database operations for engineer skills
 */
@Repository
public interface EngineerSkillRepository extends JpaRepository<EngineerSkill, Integer> {

    /**
     * Find all skills for a specific engineer
     * @param engineerId The engineer ID
     * @return List of engineer skills
     */
    List<EngineerSkill> findByEngineerId(Integer engineerId);

    /**
     * Find all engineers with a specific skill
     * @param skillId The skill ID
     * @return List of engineer skills
     */
    List<EngineerSkill> findBySkillId(Integer skillId);

    /**
     * Find skill details for a specific engineer (with join to skills table)
     * @param engineerId The engineer ID
     * @return List of SkillDTOs with complete skill information
     */
    @Query("SELECT new com.skillbridge.dto.SkillDTO(s.id, s.name, es.level, es.years) " +
            "FROM EngineerSkill es " +
            "JOIN Skill s ON es.skillId = s.id " +
            "WHERE es.engineerId = :engineerId " +
            "ORDER BY es.years DESC, s.name ASC")
    List<SkillDTO> findSkillsByEngineerId(@Param("engineerId") Integer engineerId);

    /**
     * Count skills for a specific engineer
     * @param engineerId The engineer ID
     * @return Number of skills
     */
    Long countByEngineerId(Integer engineerId);

    /**
     * Delete all skills for a specific engineer
     * @param engineerId The engineer ID
     */
    void deleteByEngineerId(Integer engineerId);
}

