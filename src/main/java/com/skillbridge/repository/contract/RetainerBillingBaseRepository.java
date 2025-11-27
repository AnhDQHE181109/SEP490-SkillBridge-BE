package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.RetainerBillingBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Retainer Billing Base Repository
 * Handles database operations for baseline billing details
 */
@Repository
public interface RetainerBillingBaseRepository extends JpaRepository<RetainerBillingBase, Integer> {
    
    /**
     * Find all baseline billing for a SOW contract, ordered by billing month descending
     * @param sowContractId SOW contract ID
     * @return List of baseline billing ordered by billing month
     */
    @Query("SELECT b FROM RetainerBillingBase b WHERE b.sowContractId = :sowContractId ORDER BY b.billingMonth DESC")
    List<RetainerBillingBase> findBySowContractIdOrderByBillingMonthDesc(@Param("sowContractId") Integer sowContractId);
    
    /**
     * Find baseline billing for a specific month
     * @param sowContractId SOW contract ID
     * @param billingMonth Billing month (YYYY-MM-01)
     * @return Optional baseline billing
     */
    Optional<RetainerBillingBase> findBySowContractIdAndBillingMonth(Integer sowContractId, LocalDate billingMonth);
    
    /**
     * Delete all baseline billing for a SOW contract
     * @param sowContractId SOW contract ID
     */
    void deleteBySowContractId(Integer sowContractId);
}

