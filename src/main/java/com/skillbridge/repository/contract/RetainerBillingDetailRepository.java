package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.RetainerBillingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Retainer Billing Detail Repository
 * Handles database operations for retainer billing details
 */
@Repository
public interface RetainerBillingDetailRepository extends JpaRepository<RetainerBillingDetail, Integer> {
    
    /**
     * Find all billing details for a SOW contract, ordered by payment_date descending
     * @param sowContractId SOW Contract ID
     * @return List of billing details
     */
    @Query("SELECT bd FROM RetainerBillingDetail bd WHERE bd.sowContractId = :sowContractId ORDER BY bd.paymentDate DESC")
    List<RetainerBillingDetail> findBySowContractIdOrderByPaymentDateDesc(@Param("sowContractId") Integer sowContractId);
}

