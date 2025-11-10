package com.skillbridge.repository.proposal;

import com.skillbridge.entity.proposal.ProposalComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Proposal Comment Repository
 * Handles database operations for proposal comments
 */
@Repository
public interface ProposalCommentRepository extends JpaRepository<ProposalComment, Integer> {

    /**
     * Find all proposal comments for a contact, ordered by creation date (newest first)
     * @param contactId Contact ID
     * @return List of proposal comments
     */
    @Query("SELECT pc FROM ProposalComment pc WHERE pc.contactId = :contactId ORDER BY pc.createdAt DESC")
    List<ProposalComment> findByContactIdOrderByCreatedAtDesc(@Param("contactId") Integer contactId);
}

