package com.skillbridge.repository.engineer;

// Temporarily commented out until Engineer entity is properly implemented
// This repository will be enabled once the Engineer entity is fully defined with JPA annotations

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
public interface EngineersRepository extends JpaRepository<Engineer, Integer> {

    /*
     This method returns the number of engineers by their statuses
     */
    Long countEngineersByStatus(String status);

//    @Query("select count(status) as Count_of_available_engineers " +
//            "from engineers e " +
//            "where e.status = 'AVAILABLE'")
//    List<Engineer> findFeaturedEngineers();

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
     * This method returns distinct primary skills fetched from engineers in the system
     * @return List<String> List of primary skills as strings
     */
    @Query("select distinct e.primarySkill " +
            "from Engineer e " +
            "where e.primarySkill is not null " +
            "order by e.primarySkill")
    List<String> findDistinctPrimarySkills();

    /**
     * This method returns distinct locations fetched from engineers in the system
     * @return List<String> List of locations as strings
     */
    @Query("select distinct e.location " +
            "from Engineer e " +
            "where e.location is not null " +
            "order by e.location")
    List<String> findDistinctLocations();

    /**
     * This method returns distinct seniority levels fetched from engineers in the system
     * @return List<String> List of seniority levels as strings
     */
    @Query("select distinct e.seniority " +
            "from Engineer e " +
            "where e.seniority is not null " +
            "order by e.seniority")
    List<String> findDistinctSeniorityLevels();

    /**
     * This method takes in parameters provided by the search criteria and performs JPQL query to return necessary data
     * @param query The search query string
     * @param primarySkill The queried primary skill
     * @param experienceMin The queried minimum experience
     * @param experienceMax The queried maximum experience
     * @param seniority The queried seniority level
     * @param location The queried location
     * @param salaryMin The queried minimum salary
     * @param salaryMax The queried maximum salary
     * @param availability The queried engineer's availability
     * @param pageable The pageable object containing pagination properties
     * @return The Page object containing queried engineers
     */
    @Query("select distinct e " +
            "from Engineer e " +
            "where (:query is null or lower(e.fullName) like lower(concat('%', :query, '%')) " +
            "        or lower(e.summary) like lower(concat('%', :query, '%')) " +
            "        or lower(e.primarySkill) like lower(concat('%', :query, '%'))) " +
            "and (:primarySkill is null or lower(e.primarySkill) like lower(concat('%', :primarySkill, '%'))) " +
            "and (:experienceMin is null or e.yearsOfExperience >= :experienceMin) " +
            "and (:experienceMax is null or e.yearsOfExperience <= :experienceMax) " +
            "and (:seniority is null or e.seniority in :seniority) " +
            "and (:location is null or e.location in :location) " +
            "and (:salaryMin is null or e.salaryExpectation >= :salaryMin) " +
            "and (:salaryMax is null or e.salaryExpectation <= :salaryMax) " +
            "and (:availability is null or :availability = false or e.status = 'AVAILABLE')")
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
}

