package com.skillbridge.service.sales;

import com.skillbridge.dto.sales.response.OpportunitiesListResponse;
import com.skillbridge.dto.sales.response.OpportunityListItemDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.opportunity.Opportunity;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.opportunity.OpportunityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SalesOpportunitiesService
 * Tests business logic for sales opportunities list with role-based filtering
 */
@ExtendWith(MockitoExtension.class)
class SalesOpportunitiesServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SalesOpportunitiesService salesOpportunitiesService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("getOpportunities - Sales Manager không có filter → trả về tất cả opportunities")
    void testGetOpportunities_SalesManager_NoFilter() {
        // Arrange
        String search = null;
        List<String> status = null;
        Integer assigneeUserId = null;
        Integer createdBy = null;
        Integer currentUserId = 1;
        String currentUserRole = "SALES_MANAGER";
        int page = 0;
        int size = 20;

        Opportunity opp1 = createOpportunity(1, "OPP-001", "New", 2, 1);
        Opportunity opp2 = createOpportunity(2, "OPP-002", "In Progress", 3, 1);
        List<Opportunity> opportunities = List.of(opp1, opp2);

        Page<Opportunity> opportunityPage = new PageImpl<>(opportunities, PageRequest.of(page, size), 2);
        when(opportunityRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(opportunityPage);
        when(userRepository.findById(2)).thenReturn(Optional.of(createUser(2, "Assignee 1", "assignee1@example.com")));
        when(userRepository.findById(3)).thenReturn(Optional.of(createUser(3, "Assignee 2", "assignee2@example.com")));
        when(userRepository.findById(1)).thenReturn(Optional.of(createUser(1, "Creator", "creator@example.com")));

        // Act
        OpportunitiesListResponse response = salesOpportunitiesService.getOpportunities(
                search, status, assigneeUserId, createdBy, currentUserId, currentUserRole, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getOpportunities().size());
        assertEquals(0, response.getPage());
        assertEquals(20, response.getPageSize());
        assertEquals(1, response.getTotalPages());
        assertEquals(2, response.getTotal());

        verify(opportunityRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getOpportunities - Sales Rep → chỉ trả về opportunities được tạo bởi họ")
    void testGetOpportunities_SalesRep_OnlyCreatedBy() {
        // Arrange
        String search = null;
        List<String> status = null;
        Integer assigneeUserId = null;
        Integer createdBy = null;
        Integer currentUserId = 1; // Sales Rep ID
        String currentUserRole = "SALES_REP";
        int page = 0;
        int size = 20;

        Opportunity opp = createOpportunity(1, "OPP-001", "New", 2, 1); // Created by user 1
        List<Opportunity> opportunities = List.of(opp);

        Page<Opportunity> opportunityPage = new PageImpl<>(opportunities, PageRequest.of(page, size), 1);
        when(opportunityRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(opportunityPage);
        when(userRepository.findById(2)).thenReturn(Optional.of(createUser(2, "Assignee 1", "assignee1@example.com")));
        when(userRepository.findById(1)).thenReturn(Optional.of(createUser(1, "Creator", "creator@example.com")));

        // Act
        OpportunitiesListResponse response = salesOpportunitiesService.getOpportunities(
                search, status, assigneeUserId, createdBy, currentUserId, currentUserRole, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getOpportunities().size());
        assertEquals(1, response.getOpportunities().get(0).getCreatedBy());

        verify(opportunityRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getOpportunities - có search query → filter theo search")
    void testGetOpportunities_WithSearchQuery() {
        // Arrange
        String search = "OPP-001";
        List<String> status = null;
        Integer assigneeUserId = null;
        Integer createdBy = null;
        Integer currentUserId = 1;
        String currentUserRole = "SALES_MANAGER";
        int page = 0;
        int size = 20;

        Opportunity opp = createOpportunity(1, "OPP-001", "New", 2, 1);
        List<Opportunity> opportunities = List.of(opp);

        Page<Opportunity> opportunityPage = new PageImpl<>(opportunities, PageRequest.of(page, size), 1);
        when(opportunityRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(opportunityPage);
        when(userRepository.findById(2)).thenReturn(Optional.of(createUser(2, "Assignee 1", "assignee1@example.com")));
        when(userRepository.findById(1)).thenReturn(Optional.of(createUser(1, "Creator", "creator@example.com")));

        // Act
        OpportunitiesListResponse response = salesOpportunitiesService.getOpportunities(
                search, status, assigneeUserId, createdBy, currentUserId, currentUserRole, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getOpportunities().size());
        assertEquals("OPP-001", response.getOpportunities().get(0).getOpportunityId());

        verify(opportunityRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getOpportunities - có status filter → filter theo status")
    void testGetOpportunities_WithStatusFilter() {
        // Arrange
        String search = null;
        List<String> status = List.of("New");
        Integer assigneeUserId = null;
        Integer createdBy = null;
        Integer currentUserId = 1;
        String currentUserRole = "SALES_MANAGER";
        int page = 0;
        int size = 20;

        Opportunity opp = createOpportunity(1, "OPP-001", "New", 2, 1);
        List<Opportunity> opportunities = List.of(opp);

        Page<Opportunity> opportunityPage = new PageImpl<>(opportunities, PageRequest.of(page, size), 1);
        when(opportunityRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(opportunityPage);
        when(userRepository.findById(2)).thenReturn(Optional.of(createUser(2, "Assignee 1", "assignee1@example.com")));
        when(userRepository.findById(1)).thenReturn(Optional.of(createUser(1, "Creator", "creator@example.com")));

        // Act
        OpportunitiesListResponse response = salesOpportunitiesService.getOpportunities(
                search, status, assigneeUserId, createdBy, currentUserId, currentUserRole, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getOpportunities().size());
        assertEquals("New", response.getOpportunities().get(0).getStatus());

        verify(opportunityRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getOpportunities - Sales Manager có assigneeUserId filter → filter theo assignee")
    void testGetOpportunities_SalesManager_WithAssigneeFilter() {
        // Arrange
        String search = null;
        List<String> status = null;
        Integer assigneeUserId = 2; // Filter by assignee
        Integer createdBy = null;
        Integer currentUserId = 1;
        String currentUserRole = "SALES_MANAGER";
        int page = 0;
        int size = 20;

        Opportunity opp = createOpportunity(1, "OPP-001", "New", 2, 1);
        List<Opportunity> opportunities = List.of(opp);

        Page<Opportunity> opportunityPage = new PageImpl<>(opportunities, PageRequest.of(page, size), 1);
        when(opportunityRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(opportunityPage);
        when(userRepository.findById(2)).thenReturn(Optional.of(createUser(2, "Assignee 1", "assignee1@example.com")));
        when(userRepository.findById(1)).thenReturn(Optional.of(createUser(1, "Creator", "creator@example.com")));

        // Act
        OpportunitiesListResponse response = salesOpportunitiesService.getOpportunities(
                search, status, assigneeUserId, createdBy, currentUserId, currentUserRole, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getOpportunities().size());
        assertEquals(2, response.getOpportunities().get(0).getAssigneeUserId());

        verify(opportunityRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getOpportunities - pagination → trả về đúng page")
    void testGetOpportunities_WithPagination() {
        // Arrange
        String search = null;
        List<String> status = null;
        Integer assigneeUserId = null;
        Integer createdBy = null;
        Integer currentUserId = 1;
        String currentUserRole = "SALES_MANAGER";
        int page = 1; // Page 1
        int size = 2; // 2 items per page

        Opportunity opp1 = createOpportunity(1, "OPP-001", "New", 2, 1);
        Opportunity opp2 = createOpportunity(2, "OPP-002", "In Progress", 3, 1);
        List<Opportunity> opportunities = List.of(opp1, opp2);

        Page<Opportunity> opportunityPage = new PageImpl<>(opportunities, PageRequest.of(page, size), 5);
        when(opportunityRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(opportunityPage);
        when(userRepository.findById(2)).thenReturn(Optional.of(createUser(2, "Assignee 1", "assignee1@example.com")));
        when(userRepository.findById(3)).thenReturn(Optional.of(createUser(3, "Assignee 2", "assignee2@example.com")));
        when(userRepository.findById(1)).thenReturn(Optional.of(createUser(1, "Creator", "creator@example.com")));

        // Act
        OpportunitiesListResponse response = salesOpportunitiesService.getOpportunities(
                search, status, assigneeUserId, createdBy, currentUserId, currentUserRole, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getPage());
        assertEquals(3, response.getTotalPages()); // 5 items / 2 per page = 3 pages
        assertEquals(5, response.getTotal());
        assertEquals(2, response.getOpportunities().size());
    }

    @Test
    @DisplayName("getOpportunities - không có opportunities → trả về empty list")
    void testGetOpportunities_NoOpportunities() {
        // Arrange
        String search = null;
        List<String> status = null;
        Integer assigneeUserId = null;
        Integer createdBy = null;
        Integer currentUserId = 1;
        String currentUserRole = "SALES_MANAGER";
        int page = 0;
        int size = 20;

        Page<Opportunity> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        when(opportunityRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        OpportunitiesListResponse response = salesOpportunitiesService.getOpportunities(
                search, status, assigneeUserId, createdBy, currentUserId, currentUserRole, page, size);

        // Assert
        assertNotNull(response);
        assertTrue(response.getOpportunities().isEmpty());
        assertEquals(0, response.getTotal());
    }

    // Helper methods
    private Opportunity createOpportunity(Integer id, String opportunityId, String status, Integer assigneeUserId, Integer createdBy) {
        Opportunity opportunity = new Opportunity();
        opportunity.setId(id);
        opportunity.setOpportunityId(opportunityId);
        opportunity.setStatus(status);
        opportunity.setAssigneeUserId(assigneeUserId);
        opportunity.setCreatedBy(createdBy);
        opportunity.setClientName("Client Name");
        opportunity.setClientEmail("client@example.com");
        opportunity.setEstValue(java.math.BigDecimal.valueOf(100000));
        opportunity.setCurrency("USD");
        opportunity.setProbability(50);
        opportunity.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return opportunity;
    }

    private User createUser(Integer id, String fullName, String email) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone("090-1234-5678");
        user.setCompanyName("Test Company");
        return user;
    }
}

