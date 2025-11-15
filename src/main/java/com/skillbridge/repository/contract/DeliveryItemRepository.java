package com.skillbridge.repository.contract;

import com.skillbridge.entity.contract.DeliveryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Delivery Item Repository
 * Handles database operations for delivery items
 */
@Repository
public interface DeliveryItemRepository extends JpaRepository<DeliveryItem, Integer> {
    
    /**
     * Find all delivery items for a SOW contract, ordered by payment_date descending
     * @param sowContractId SOW Contract ID
     * @return List of delivery items
     */
    @Query("SELECT di FROM DeliveryItem di WHERE di.sowContractId = :sowContractId ORDER BY di.paymentDate DESC")
    List<DeliveryItem> findBySowContractIdOrderByPaymentDateDesc(@Param("sowContractId") Integer sowContractId);
}

