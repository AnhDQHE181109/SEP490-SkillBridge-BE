package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.FixedPriceBillingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Fixed Price Billing Detail Repository
 * Handles database operations for fixed price billing details
 */
@Repository
public interface FixedPriceBillingDetailRepository extends JpaRepository<FixedPriceBillingDetail, Integer> {
    
    /**
     * Find all billing details for a SOW contract, ordered by invoice_date descending
     * @param sowContractId SOW Contract ID
     * @return List of billing details
     */
    @Query("SELECT bd FROM FixedPriceBillingDetail bd WHERE bd.sowContractId = :sowContractId ORDER BY bd.invoiceDate DESC")
    List<FixedPriceBillingDetail> findBySowContractIdOrderByInvoiceDateDesc(@Param("sowContractId") Integer sowContractId);
}

