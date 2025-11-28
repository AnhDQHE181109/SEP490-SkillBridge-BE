package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.CRBillingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * CR Billing Event Repository
 * Handles database operations for billing change events
 */
@Repository
public interface CRBillingEventRepository extends JpaRepository<CRBillingEvent, Integer> {
    
    /**
     * Find all billing events for a change request
     * @param changeRequestId Change request ID
     * @return List of billing events
     */
    List<CRBillingEvent> findByChangeRequestId(Integer changeRequestId);
    
    /**
     * Find all billing events for a specific month
     * @param sowContractId SOW contract ID
     * @param billingMonth Billing month (YYYY-MM-01)
     * @return List of billing events for the month
     */
    @Query("SELECT e FROM CRBillingEvent e " +
           "INNER JOIN ChangeRequest cr ON e.changeRequestId = cr.id " +
           "WHERE cr.sowContractId = :sowContractId " +
           "AND cr.status IN ('APPROVED', 'Active') " +
           "AND e.billingMonth = :billingMonth " +
           "ORDER BY e.createdAt ASC")
    List<CRBillingEvent> findApprovedEventsByMonth(@Param("sowContractId") Integer sowContractId, 
                                                    @Param("billingMonth") LocalDate billingMonth);
    
    /**
     * Find all billing events for approved change requests of a SOW contract
     * @param sowContractId SOW contract ID
     * @return List of billing events
     */
    @Query("SELECT e FROM CRBillingEvent e " +
           "INNER JOIN ChangeRequest cr ON e.changeRequestId = cr.id " +
           "WHERE cr.sowContractId = :sowContractId " +
           "AND cr.status IN ('APPROVED', 'Active') " +
           "ORDER BY e.billingMonth ASC, e.createdAt ASC")
    List<CRBillingEvent> findApprovedEventsBySowContractId(@Param("sowContractId") Integer sowContractId);
}

