package com.skillbridge.repository.proposal;

import com.skillbridge.entity.proposal.Proposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Proposal Repository
 * Handles database operations for proposals
 */
@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Integer> {

    /**
     * Find proposals for a client with search and filter
     * Search across proposal title, contact_id (from contact), and contact description
     * Filter by status if provided
     * Filter by client_user_id through contact relationship
     * Note: Using JOIN instead of JOIN FETCH because Pageable doesn't work with JOIN FETCH
     */
    @Query("SELECT p FROM Proposal p " +
           "JOIN p.contact c " +
           "WHERE c.clientUserId = :clientUserId " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     CONCAT(p.contactId, '') LIKE CONCAT('%', :search, '%')) " +
           "AND (:status IS NULL OR :status = '' OR p.status = :status) " +
           "ORDER BY p.createdAt DESC")
    Page<Proposal> findProposalsForClient(
        @Param("clientUserId") Integer clientUserId,
        @Param("search") String search,
        @Param("status") String status,
        Pageable pageable
    );

    /**
     * Find proposal by ID and client user ID (through contact)
     */
    @Query("SELECT p FROM Proposal p " +
           "JOIN p.contact c " +
           "WHERE p.id = :proposalId AND c.clientUserId = :clientUserId")
    Optional<Proposal> findByIdAndClientUserId(
        @Param("proposalId") Integer proposalId,
        @Param("clientUserId") Integer clientUserId
    );

    /**
     * Find proposals by contact ID
     */
    List<Proposal> findByContactId(Integer contactId);

    /**
     * Count proposals by year for ID generation
     */
    @Query("SELECT COUNT(p) FROM Proposal p WHERE YEAR(p.createdAt) = :year")
    Long countByYear(@Param("year") Integer year);
}

