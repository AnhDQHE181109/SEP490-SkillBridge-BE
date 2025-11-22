package com.skillbridge.repository.proposal;

import com.skillbridge.entity.proposal.ProposalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProposalHistoryRepository extends JpaRepository<ProposalHistory, Integer> {
    List<ProposalHistory> findByOpportunityIdOrderByCreatedAtDesc(Integer opportunityId);
    List<ProposalHistory> findByProposalIdOrderByCreatedAtDesc(Integer proposalId);
}

