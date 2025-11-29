package com.skillbridge.repository.engineer;

import com.skillbridge.entity.engineer.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Skill Repository
 * Handles database operations for skills
 */
@Repository
public interface SkillRepository extends JpaRepository<Skill, Integer> {

    /**
     * Find all parent skills (skills with parent_skill_id IS NULL)
     * @param pageable Pagination information
     * @return Page of parent skills
     */
    Page<Skill> findByParentSkillIdIsNull(Pageable pageable);

    /**
     * Find all sub-skills by parent skill ID
     * @param parentSkillId The parent skill ID
     * @return List of sub-skills
     */
    List<Skill> findByParentSkillId(Integer parentSkillId);

    /**
     * Search parent skills by name (case-insensitive)
     * @param search Search term
     * @param pageable Pagination information
     * @return Page of matching parent skills
     */
    @Query("SELECT s FROM Skill s WHERE s.parentSkillId IS NULL " +
           "AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(s.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Skill> searchParentSkills(@Param("search") String search, Pageable pageable);

    /**
     * Find parent skills by name (case-insensitive) and parent is null
     * @param search Search term
     * @param pageable Pagination information
     * @return Page of matching parent skills
     */
    Page<Skill> findByNameContainingIgnoreCaseAndParentSkillIdIsNull(String search, Pageable pageable);

    /**
     * Find skill by name (for uniqueness check)
     * @param name Skill name
     * @return Optional Skill
     */
    Optional<Skill> findByName(String name);

    /**
     * Find sub-skill by name and parent skill ID (for uniqueness check)
     * @param name Sub-skill name
     * @param parentSkillId Parent skill ID
     * @return Optional Skill
     */
    Optional<Skill> findByNameAndParentSkillId(String name, Integer parentSkillId);

    /**
     * Find parent skill by name and parent is null
     * @param name Skill name
     * @return Optional Skill
     */
    Optional<Skill> findByNameAndParentSkillIdIsNull(String name);

    /**
     * Count sub-skills by parent skill ID
     * @param parentSkillId Parent skill ID
     * @return Count of sub-skills
     */
    Long countByParentSkillId(Integer parentSkillId);
}

