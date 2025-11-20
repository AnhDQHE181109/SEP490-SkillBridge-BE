package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.ChangeRequestEngagedEngineer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Change Request Engaged Engineer Repository
 * Handles database operations for change request engaged engineers
 */
@Repository
public interface ChangeRequestEngagedEngineerRepository extends JpaRepository<ChangeRequestEngagedEngineer, Integer> {
    
    /**
     * Find all engaged engineers for a change request
     * @param changeRequestId Change request ID
     * @return List of engaged engineers
     */
    List<ChangeRequestEngagedEngineer> findByChangeRequestId(Integer changeRequestId);
    
    /**
     * Delete all engaged engineers for a change request
     * @param changeRequestId Change request ID
     */
    void deleteByChangeRequestId(Integer changeRequestId);
}

