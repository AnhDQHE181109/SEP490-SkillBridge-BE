package com.skillbridge.repository;

// Temporarily commented out until Engineer entity is properly implemented
// This repository will be enabled once the Engineer entity is fully defined with JPA annotations

import com.skillbridge.entity.Engineer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EngineersRepository extends JpaRepository<Engineer, Integer> {

    /*
     This method returns the number of engineers by their statuses
     */
    Long countEngineersByStatus(String status);

//    @Query("select count(status) as Count_of_available_engineers\n" +
//            "from engineers e\n" +
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


}

