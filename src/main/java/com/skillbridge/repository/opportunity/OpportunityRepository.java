package com.skillbridge.repository.opportunity;

import com.skillbridge.entity.opportunity.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Integer>, JpaSpecificationExecutor<Opportunity> {
    Optional<Opportunity> findByOpportunityId(String opportunityId);

    /**
     * Find opportunities by contact ID
     */
    List<Opportunity> findByContactId(Integer contactId);
}

