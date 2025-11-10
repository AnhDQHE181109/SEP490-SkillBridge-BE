package com.skillbridge.repository.contact;

import com.skillbridge.entity.contact.ConsultationCancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Consultation Cancellation Repository
 * Handles database operations for consultation cancellations
 */
@Repository
public interface ConsultationCancellationRepository extends JpaRepository<ConsultationCancellation, Integer> {

    /**
     * Find cancellation for a contact
     * @param contactId Contact ID
     * @return Optional cancellation
     */
    @Query("SELECT cc FROM ConsultationCancellation cc WHERE cc.contactId = :contactId ORDER BY cc.cancelledAt DESC")
    Optional<ConsultationCancellation> findFirstByContactIdOrderByCancelledAtDesc(@Param("contactId") Integer contactId);
}

