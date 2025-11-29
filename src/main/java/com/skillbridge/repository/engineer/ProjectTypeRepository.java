package com.skillbridge.repository.engineer;

import com.skillbridge.entity.engineer.ProjectType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Project Type Repository
 * Handles database operations for project types
 */
@Repository
public interface ProjectTypeRepository extends JpaRepository<ProjectType, Integer> {

    /**
     * Search project types by name (case-insensitive)
     * @param search Search term
     * @param pageable Pagination information
     * @return Page of matching project types
     */
    @Query("SELECT pt FROM ProjectType pt WHERE " +
           "LOWER(pt.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(pt.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<ProjectType> searchProjectTypes(@Param("search") String search, Pageable pageable);

    /**
     * Find project types by name containing (case-insensitive)
     * @param search Search term
     * @param pageable Pagination information
     * @return Page of matching project types
     */
    Page<ProjectType> findByNameContainingIgnoreCase(String search, Pageable pageable);

    /**
     * Find project type by name (for uniqueness check)
     * @param name Project type name
     * @return Optional ProjectType
     */
    Optional<ProjectType> findByName(String name);

    /**
     * Check if project type exists by name
     * @param name Project type name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);
}

