package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.ProjectCloseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Project Close Request Repository
 * Handles database operations for project close requests
 * Story-41: Project Close Request for SOW Contract
 */
@Repository
public interface ProjectCloseRequestRepository extends JpaRepository<ProjectCloseRequest, Integer> {
    
    /**
     * Find close request by SOW ID and status
     */
    Optional<ProjectCloseRequest> findBySowIdAndStatus(
        Integer sowId, 
        ProjectCloseRequest.ProjectCloseRequestStatus status
    );
    
    /**
     * Find the latest close request for a SOW, ordered by creation date descending
     */
    @Query("SELECT pcr FROM ProjectCloseRequest pcr WHERE pcr.sowId = :sowId " +
           "ORDER BY pcr.createdAt DESC")
    List<ProjectCloseRequest> findBySowIdOrderByCreatedAtDesc(@Param("sowId") Integer sowId);
    
    /**
     * Find the first (latest) close request for a SOW, ordered by creation date descending
     */
    @Query("SELECT pcr FROM ProjectCloseRequest pcr WHERE pcr.sowId = :sowId " +
           "ORDER BY pcr.createdAt DESC")
    Optional<ProjectCloseRequest> findFirstBySowIdOrderByCreatedAtDesc(@Param("sowId") Integer sowId);
    
    /**
     * Check if a pending close request exists for a SOW
     */
    boolean existsBySowIdAndStatus(
        Integer sowId, 
        ProjectCloseRequest.ProjectCloseRequestStatus status
    );
    
    /**
     * Find all close requests for a SOW
     */
    List<ProjectCloseRequest> findBySowId(Integer sowId);
}

