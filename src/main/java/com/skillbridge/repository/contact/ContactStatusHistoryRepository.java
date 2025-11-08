package com.skillbridge.repository.contact;

import com.skillbridge.entity.contact.ContactStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Contact Status History Repository
 * Handles database operations for contact status history
 */
@Repository
public interface ContactStatusHistoryRepository extends JpaRepository<ContactStatusHistory, Integer> {

    /**
     * Find status history by contact ID
     * @param contactId Contact ID
     * @return List of status history records
     */
    List<ContactStatusHistory> findByContactId(Integer contactId);
}

