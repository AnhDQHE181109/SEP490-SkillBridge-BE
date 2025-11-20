package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.ChangeRequestBillingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Change Request Billing Detail Repository
 * Handles database operations for change request billing details
 */
@Repository
public interface ChangeRequestBillingDetailRepository extends JpaRepository<ChangeRequestBillingDetail, Integer> {
    
    /**
     * Find all billing details for a change request
     * @param changeRequestId Change request ID
     * @return List of billing details
     */
    List<ChangeRequestBillingDetail> findByChangeRequestId(Integer changeRequestId);
    
    /**
     * Delete all billing details for a change request
     * @param changeRequestId Change request ID
     */
    void deleteByChangeRequestId(Integer changeRequestId);
}

