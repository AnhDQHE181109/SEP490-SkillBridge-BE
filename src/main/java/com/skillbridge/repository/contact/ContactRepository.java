package com.skillbridge.repository.contact;

import com.skillbridge.entity.contact.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Contact Repository
 * Handles database operations for contacts
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, Integer> {

    /**
     * Find contacts by client user ID
     * @param userId Client user ID
     * @return List of contacts
     */
    List<Contact> findByClientUserId(Integer userId);

    /**
     * Find contacts by client user ID ordered by created date
     * @param userId Client user ID
     * @return List of contacts
     */
    @Query("SELECT c FROM Contact c WHERE c.clientUserId = :userId ORDER BY c.createdAt DESC")
    List<Contact> findByClientUserIdOrderByCreatedAtDesc(@Param("userId") Integer userId);

    /**
     * Find contacts by status ordered by created date
     * @param status Contact status
     * @return List of contacts
     */
    @Query("SELECT c FROM Contact c WHERE c.status = :status ORDER BY c.createdAt DESC")
    List<Contact> findByStatusOrderByCreatedAtDesc(@Param("status") String status);

    /**
     * Count number of new contacts in the system
     * @param status Contact status
     * @return Number of new contacts
     */
    Long countByStatus(String status);
}

