package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.CRResourceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * CR Resource Event Repository
 * Handles database operations for resource change events
 */
@Repository
public interface CRResourceEventRepository extends JpaRepository<CRResourceEvent, Integer> {
    
    /**
     * Find all resource events for a change request
     * @param changeRequestId Change request ID
     * @return List of resource events
     */
    List<CRResourceEvent> findByChangeRequestId(Integer changeRequestId);
    
    /**
     * Find all resource events for a SOW contract up to a specific date
     * @param sowContractId SOW contract ID
     * @param asOfDate Date to check up to
     * @return List of resource events
     */
    @Query("SELECT e FROM CRResourceEvent e " +
           "WHERE e.changeRequestId IN (" +
           "  SELECT cr.id FROM ChangeRequest cr " +
           "  WHERE cr.sowContractId = :sowContractId " +
           "  AND UPPER(cr.status) IN ('APPROVED', 'ACTIVE')" +
           ") " +
           "AND e.effectiveStart <= :asOfDate " +
           "ORDER BY e.effectiveStart ASC, e.createdAt ASC")
    List<CRResourceEvent> findApprovedEventsUpToDate(@Param("sowContractId") Integer sowContractId, 
                                                      @Param("asOfDate") LocalDate asOfDate);
    
    /**
     * Find all resource events for approved change requests of a SOW contract
     * @param sowContractId SOW contract ID
     * @return List of resource events
     */
    @Query("SELECT e FROM CRResourceEvent e " +
           "WHERE e.changeRequestId IN (" +
           "  SELECT cr.id FROM ChangeRequest cr " +
           "  WHERE cr.sowContractId = :sowContractId " +
           "  AND UPPER(cr.status) IN ('APPROVED', 'ACTIVE')" +
           ") " +
           "ORDER BY e.effectiveStart ASC, e.createdAt ASC")
    List<CRResourceEvent> findApprovedEventsBySowContractId(@Param("sowContractId") Integer sowContractId);
    
    /**
     * Debug: Find all resource events for a SOW contract (without status filter)
     * @param sowContractId SOW contract ID
     * @return List of resource events
     */
    @Query("SELECT e FROM CRResourceEvent e " +
           "WHERE e.changeRequestId IN (" +
           "  SELECT cr.id FROM ChangeRequest cr " +
           "  WHERE cr.sowContractId = :sowContractId" +
           ") " +
           "ORDER BY e.effectiveStart ASC, e.createdAt ASC")
    List<CRResourceEvent> findAllEventsBySowContractId(@Param("sowContractId") Integer sowContractId);
}

