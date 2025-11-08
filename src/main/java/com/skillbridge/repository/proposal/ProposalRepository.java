package com.skillbridge.repository.proposal;

import com.skillbridge.entity.proposal.Proposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal,Integer> {

    @Query("select p " +
            "from Proposal p " +
            "join p.contact c " +
            "where c.clientUserId = :clientUserId " +
            "and (:search is null or :search = '' or " +
            "   lower(p.title) like lower(concat('%', :search, '%')) or " +
            "   lower(c.description) like lower(concat('%', :search, '%')) or " +
            "   lower(concat(p.contactID, '')) like lower(concat('%', :search, '%'))) " +
            "and (:status is null or :status = '' or :status = p.status) " +
            "order by p.createdAt desc")
    Page<Proposal> findProposalsForClient(
            @Param("clientUserId") Integer clientUserId,
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable
    );

}
