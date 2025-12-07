package com.skillbridge.service.sales;

import com.skillbridge.dto.sales.response.OpportunitiesListResponse;
import com.skillbridge.dto.sales.response.OpportunityListItemDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.opportunity.Opportunity;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.opportunity.OpportunityRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sales Opportunities Service
 * Handles business logic for sales opportunities list with role-based filtering
 */
@Service
public class SalesOpportunitiesService {

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get opportunities for sales users with role-based filtering
     * Sales Manager: all opportunities
     * Sales Man: only opportunities created by them
     */
    public OpportunitiesListResponse getOpportunities(
            String search,
            List<String> status,
            Integer assigneeUserId, // For Sales Manager filtering
            Integer createdBy, // For Sales Manager filtering
            Integer currentUserId,
            String currentUserRole,
            int page,
            int size
    ) {
        // Build specification with role-based filtering
        Specification<Opportunity> spec = buildSpecification(search, status, assigneeUserId, createdBy, currentUserId, currentUserRole);

        // Create pageable with sorting by createdAt descending
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Execute query
        Page<Opportunity> opportunityPage = opportunityRepository.findAll(spec, pageable);

        // Convert to DTOs
        List<OpportunityListItemDTO> opportunities = opportunityPage.getContent().stream()
                .map(opportunity -> convertToDTO(opportunity))
                .collect(Collectors.toList());

        // Load assignee and creator names
        for (OpportunityListItemDTO dto : opportunities) {
            if (dto.getAssigneeUserId() != null) {
                userRepository.findById(dto.getAssigneeUserId()).ifPresent(user -> {
                    dto.setAssigneeName(user.getFullName());
                });
            }
            if (dto.getCreatedBy() != null) {
                userRepository.findById(dto.getCreatedBy()).ifPresent(user -> {
                    dto.setCreatedByName(user.getFullName());
                });
            }
        }

        // Build response
        OpportunitiesListResponse response = new OpportunitiesListResponse();
        response.setOpportunities(opportunities);
        response.setPage(page);
        response.setPageSize(size);
        response.setTotalPages(opportunityPage.getTotalPages());
        response.setTotal(opportunityPage.getTotalElements());

        return response;
    }

    /**
     * Build JPA Specification for filtering opportunities
     */
    private Specification<Opportunity> buildSpecification(
            String search,
            List<String> status,
            Integer assigneeUserId,
            Integer createdBy,
            Integer currentUserId,
            String currentUserRole
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Role-based filtering: Sales Man only sees opportunities they created
            if ("SALES_REP".equals(currentUserRole)) {
                predicates.add(cb.equal(root.get("createdBy"), currentUserId));
            } else if (createdBy != null) {
                // Sales Manager can filter by creator
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            }

            // Assignee filter (for Sales Manager)
            if (assigneeUserId != null && "SALES_MANAGER".equals(currentUserRole)) {
                predicates.add(cb.equal(root.get("assigneeUserId"), assigneeUserId));
            }

            // Search filter (searches in opportunity ID, client name, client email)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";

                Predicate opportunityIdPredicate = cb.like(cb.lower(root.get("opportunityId")), searchPattern);
                Predicate clientNamePredicate = cb.like(cb.lower(root.get("clientName")), searchPattern);
                Predicate clientEmailPredicate = cb.like(cb.lower(root.get("clientEmail")), searchPattern);

                predicates.add(cb.or(
                    opportunityIdPredicate,
                    clientNamePredicate,
                    clientEmailPredicate
                ));
            }

            // Status filter
            if (status != null && !status.isEmpty()) {
                predicates.add(root.get("status").in(status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Convert Opportunity entity to DTO
     */
    private OpportunityListItemDTO convertToDTO(Opportunity opportunity) {
        OpportunityListItemDTO dto = new OpportunityListItemDTO();
        dto.setId(opportunity.getId());
        dto.setOpportunityId(opportunity.getOpportunityId());
        dto.setEstValue(opportunity.getEstValue());
        dto.setCurrency(opportunity.getCurrency());
        dto.setProbability(opportunity.getProbability());
        dto.setClientEmail(opportunity.getClientEmail());
        dto.setClientName(opportunity.getClientName());
        dto.setStatus(opportunity.getStatus());
        dto.setAssigneeUserId(opportunity.getAssigneeUserId());
        dto.setCreatedBy(opportunity.getCreatedBy());
        return dto;
    }
}

