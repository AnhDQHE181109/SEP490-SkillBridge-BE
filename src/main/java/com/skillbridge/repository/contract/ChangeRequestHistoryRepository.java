package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.ChangeRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Change Request History Repository
 * Handles database operations for change request history
 */
@Repository
public interface ChangeRequestHistoryRepository extends JpaRepository<ChangeRequestHistory, Integer> {
    
    /**
     * Find all history entries for a change request, ordered by timestamp descending
     * @param changeRequestId Change request ID
     * @return List of history entries
     */
    List<ChangeRequestHistory> findByChangeRequestIdOrderByTimestampDesc(Integer changeRequestId);
}

