package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.ChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Change Request Repository
 * Handles database operations for change requests
 */
@Repository
public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Integer> {
    
    /**
     * Find all change requests for an MSA contract, ordered by creation date descending
     */
    @Query("SELECT cr FROM ChangeRequest cr WHERE cr.contractId = :contractId " +
           "ORDER BY cr.createdAt DESC")
    List<ChangeRequest> findByContractIdOrderByCreatedAtDesc(@Param("contractId") Integer contractId);
    
    /**
     * Find all change requests for a SOW contract, ordered by creation date descending
     */
    @Query("SELECT cr FROM ChangeRequest cr WHERE cr.sowContractId = :sowContractId " +
           "ORDER BY cr.createdAt DESC")
    List<ChangeRequest> findBySowContractIdOrderByCreatedAtDesc(@Param("sowContractId") Integer sowContractId);
}

