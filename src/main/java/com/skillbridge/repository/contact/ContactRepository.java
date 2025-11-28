package com.skillbridge.repository.contact;

import com.skillbridge.entity.contact.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Contact Repository
 * Handles database operations for contacts
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, Integer>, JpaSpecificationExecutor<Contact> {

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
     * Search and filter contacts for client with pagination
     * @param clientUserId Client user ID (for security)
     * @param search Search query (searches in title, description, contact_id)
     * @param status Status filter (null for all)
     * @param pageable Pagination parameters
     * @return Page of contacts
     */
    @Query("SELECT c FROM Contact c WHERE " +
           "c.clientUserId = :clientUserId " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR :status = '' OR c.status = :status) " +
           "ORDER BY c.createdAt DESC")
    Page<Contact> findContactsForClient(
        @Param("clientUserId") Integer clientUserId,
        @Param("search") String search,
        @Param("status") String status,
        Pageable pageable
    );

    /**
     * Check if contact has contact_id field (for migration compatibility)
     */
    @Query(value = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
           "WHERE TABLE_SCHEMA = DATABASE() " +
           "AND TABLE_NAME = 'contacts' " +
           "AND COLUMN_NAME = 'contact_id'", nativeQuery = true)
    int hasContactIdColumn();

    /**
     * Find contact by ID and client user ID (for security validation)
     * @param id Contact ID
     * @param clientUserId Client user ID
     * @return Optional contact
     */
    @Query("SELECT c FROM Contact c WHERE c.id = :id AND c.clientUserId = :clientUserId")
    Optional<Contact> findByIdAndClientUserId(@Param("id") Integer id, @Param("clientUserId") Integer clientUserId);
}

