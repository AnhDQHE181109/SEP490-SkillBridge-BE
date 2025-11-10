package com.skillbridge.repository.contact;

import com.skillbridge.entity.contact.CommunicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Communication Log Repository
 * Handles database operations for communication logs
 */
@Repository
public interface CommunicationLogRepository extends JpaRepository<CommunicationLog, Integer> {

    /**
     * Find all communication logs for a contact, ordered by creation date (newest first)
     * @param contactId Contact ID
     * @return List of communication logs
     */
    @Query("SELECT cl FROM CommunicationLog cl WHERE cl.contactId = :contactId ORDER BY cl.createdAt DESC")
    List<CommunicationLog> findByContactIdOrderByCreatedAtDesc(@Param("contactId") Integer contactId);

    /**
     * Find all communication logs for a contact, ordered by creation date (oldest first)
     * @param contactId Contact ID
     * @return List of communication logs
     */
    @Query("SELECT cl FROM CommunicationLog cl WHERE cl.contactId = :contactId ORDER BY cl.createdAt ASC")
    List<CommunicationLog> findByContactIdOrderByCreatedAtAsc(@Param("contactId") Integer contactId);
}

