package com.skillbridge.repository.engineer;

import com.skillbridge.entity.engineer.Engineer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface EngineerRepository extends JpaRepository<Engineer, Integer> {
    
    /**
     * Count engineers by status
     */
    Long countByStatus(String status);

    /**
     * Find featured engineers for homepage (available engineers)
     */
    @Query("SELECT e FROM Engineer e WHERE e.status = 'AVAILABLE' ORDER BY e.createdAt DESC")
    List<Engineer> findFeaturedEngineers();

    /**
     * Find engineers by category based on primary skill
     */
    @Query("SELECT e FROM Engineer e WHERE e.status = 'AVAILABLE' " +
           "AND (LOWER(e.primarySkill) LIKE LOWER(CONCAT('%', :category, '%')) " +
           "OR LOWER(e.summary) LIKE LOWER(CONCAT('%', :category, '%')))")
    List<Engineer> findByCategory(@Param("category") String category);

    /**
     * Find web development engineers
     */
    @Query("SELECT e FROM Engineer e WHERE e.status = 'AVAILABLE' " +
           "AND (LOWER(e.primarySkill) LIKE '%web%' " +
           "OR LOWER(e.primarySkill) LIKE '%frontend%' " +
           "OR LOWER(e.primarySkill) LIKE '%backend%' " +
           "OR LOWER(e.primarySkill) LIKE '%react%' " +
           "OR LOWER(e.primarySkill) LIKE '%angular%' " +
           "OR LOWER(e.primarySkill) LIKE '%vue%') " +
           "ORDER BY e.createdAt DESC")
    List<Engineer> findWebDevelopers();

    /**
     * Find game development engineers
     */
    @Query("SELECT e FROM Engineer e WHERE e.status = 'AVAILABLE' " +
           "AND (LOWER(e.primarySkill) LIKE '%game%' " +
           "OR LOWER(e.primarySkill) LIKE '%unity%' " +
           "OR LOWER(e.primarySkill) LIKE '%unreal%' " +
           "OR LOWER(e.primarySkill) LIKE '%godot%') " +
           "ORDER BY e.createdAt DESC")
    List<Engineer> findGameDevelopers();

    /**
     * Find AI/ML development engineers
     */
    @Query("SELECT e FROM Engineer e WHERE e.status = 'AVAILABLE' " +
           "AND (LOWER(e.primarySkill) LIKE '%ai%' " +
           "OR LOWER(e.primarySkill) LIKE '%ml%' " +
           "OR LOWER(e.primarySkill) LIKE '%machine learning%' " +
           "OR LOWER(e.primarySkill) LIKE '%artificial intelligence%' " +
           "OR LOWER(e.primarySkill) LIKE '%deep learning%' " +
           "OR LOWER(e.primarySkill) LIKE '%data science%') " +
           "ORDER BY e.createdAt DESC")
    List<Engineer> findAiMlDevelopers();

    /**
     * Advanced search with dynamic filters
     */
    @Query("SELECT DISTINCT e FROM Engineer e " +
           "WHERE (:query IS NULL OR LOWER(e.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "   OR LOWER(e.summary) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "   OR LOWER(e.primarySkill) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:primarySkill IS NULL OR LOWER(e.primarySkill) LIKE LOWER(CONCAT('%', :primarySkill, '%'))) " +
           "AND (:experienceMin IS NULL OR e.yearsExperience >= :experienceMin) " +
           "AND (:experienceMax IS NULL OR e.yearsExperience <= :experienceMax) " +
           "AND (:seniority IS NULL OR e.seniority IN :seniority) " +
           "AND (:location IS NULL OR e.location IN :location) " +
           "AND (:salaryMin IS NULL OR e.salaryExpectation >= :salaryMin) " +
           "AND (:salaryMax IS NULL OR e.salaryExpectation <= :salaryMax) " +
           "AND (:availability IS NULL OR :availability = false OR e.status = 'AVAILABLE')")
    Page<Engineer> searchEngineers(
        @Param("query") String query,
        @Param("primarySkill") String primarySkill,
        @Param("experienceMin") Integer experienceMin,
        @Param("experienceMax") Integer experienceMax,
        @Param("seniority") List<String> seniority,
        @Param("location") List<String> location,
        @Param("salaryMin") BigDecimal salaryMin,
        @Param("salaryMax") BigDecimal salaryMax,
        @Param("availability") Boolean availability,
        Pageable pageable
    );

    /**
     * Get distinct primary skills
     */
    @Query("SELECT DISTINCT e.primarySkill FROM Engineer e WHERE e.primarySkill IS NOT NULL ORDER BY e.primarySkill")
    List<String> findDistinctPrimarySkills();

    /**
     * Get distinct locations
     */
    @Query("SELECT DISTINCT e.location FROM Engineer e WHERE e.location IS NOT NULL ORDER BY e.location")
    List<String> findDistinctLocations();

    /**
     * Get distinct seniority levels
     */
    @Query("SELECT DISTINCT e.seniority FROM Engineer e WHERE e.seniority IS NOT NULL ORDER BY e.seniority")
    List<String> findDistinctSeniorities();

    /**
     * Search engineers by full name or primary skill (for Admin list)
     */
    Page<Engineer> findByFullNameContainingIgnoreCaseOrPrimarySkillContainingIgnoreCase(
            String fullName, String primarySkill, Pageable pageable);

    /**
     * Find engineer by email (for uniqueness check)
     */
    java.util.Optional<Engineer> findByEmail(String email);
}

