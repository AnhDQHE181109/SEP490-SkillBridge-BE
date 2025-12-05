package com.skillbridge.service.sales;

import com.skillbridge.dto.sales.response.SalesDashboardSummaryDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.entity.contract.ChangeRequest;
import com.skillbridge.entity.contract.Contract;
import com.skillbridge.entity.contract.SOWContract;
import com.skillbridge.entity.opportunity.Opportunity;
import com.skillbridge.entity.proposal.Proposal;
import com.skillbridge.repository.contact.ContactRepository;
import com.skillbridge.repository.contract.ChangeRequestRepository;
import com.skillbridge.repository.contract.ContractRepository;
import com.skillbridge.repository.contract.SOWContractRepository;
import com.skillbridge.repository.opportunity.OpportunityRepository;
import com.skillbridge.repository.proposal.ProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SalesDashboardService
 * Tests business logic for Sales Dashboard data aggregation with role-based filtering
 */
@ExtendWith(MockitoExtension.class)
class SalesDashboardServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private SOWContractRepository sowContractRepository;

    @Mock
    private ChangeRequestRepository changeRequestRepository;

    @InjectMocks
    private SalesDashboardService salesDashboardService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("getSummary - Sales Manager → trả về tất cả data")
    void testGetSummary_SalesManager() {
        // Arrange
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");
        Contact contact1 = createContact(1, "New");
        Contact contact2 = createContact(2, "In Progress");
        List<Contact> contacts = List.of(contact1, contact2);

        Opportunity opp1 = createOpportunity(1, "NEW");
        Opportunity opp2 = createOpportunity(2, "PROPOSAL_DRAFTING");
        List<Opportunity> opportunities = List.of(opp1, opp2);

        Proposal proposal1 = createProposal(1, "draft");
        Proposal proposal2 = createProposal(2, "revision_requested");
        List<Proposal> proposals = List.of(proposal1, proposal2);

        Contract contract1 = createMSAContract(1, Contract.ContractStatus.Active);
        List<Contract> msaContracts = List.of(contract1);

        SOWContract sowContract1 = createSOWContract(1, SOWContract.SOWContractStatus.Active);
        List<SOWContract> sowContracts = List.of(sowContract1);

        ChangeRequest cr1 = createChangeRequest(1, "Approved");
        List<ChangeRequest> changeRequests = List.of(cr1);

        // Mock getContacts - Sales Manager gets all
        when(contactRepository.findAll()).thenReturn(contacts);
        // Mock getOpportunities - Sales Manager gets all
        when(opportunityRepository.findAll()).thenReturn(opportunities);
        // Mock getProposals - Sales Manager gets all
        when(proposalRepository.findAll()).thenReturn(proposals);
        // Mock getMSAContracts - Sales Manager gets all
        when(contractRepository.findAll()).thenReturn(msaContracts);
        // Mock getSOWContracts - Sales Manager gets all
        when(sowContractRepository.findAll()).thenReturn(sowContracts);
        // Mock getChangeRequests - Sales Manager gets all
        when(changeRequestRepository.findAll()).thenReturn(changeRequests);

        // Act
        SalesDashboardSummaryDTO result = salesDashboardService.getSummary(currentUser);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContacts().getAll());
        assertEquals(1, result.getContacts().getNewCount());
        assertEquals(2, result.getOpportunities().getAll());
        assertEquals(1, result.getOpportunities().getUnderReview());
        assertEquals(2, result.getProposals().getAll());
        assertEquals(1, result.getProposals().getUnderReview());
        assertEquals(2, result.getContracts().getAll()); // MSA + SOW
        assertEquals(1, result.getChangeRequests().getAll()); // Only 1 change request
    }

    @Test
    @DisplayName("getSummary - Sales Rep → chỉ trả về assigned data")
    void testGetSummary_SalesRep() {
        // Arrange
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");
        Contact contact1 = createContact(1, "New", 1); // Assigned to user 1
        Contact contact2 = createContact(2, "New", 2); // Assigned to user 2
        List<Contact> allContacts = List.of(contact1, contact2);

        Opportunity opp1 = createOpportunity(1, "NEW", 1); // Assigned to user 1
        Opportunity opp2 = createOpportunity(2, "NEW", 2); // Assigned to user 2
        List<Opportunity> allOpportunities = List.of(opp1, opp2);

        Proposal proposal1 = createProposal(1, "draft", 1); // Linked to opp1
        proposal1.setOpportunityId(1);
        List<Proposal> allProposals = List.of(proposal1);

        Contract contract1 = createMSAContract(1, Contract.ContractStatus.Active, 1); // Assigned to user 1
        Contract contract2 = createMSAContract(2, Contract.ContractStatus.Active, 2); // Assigned to user 2
        List<Contract> allMSAContracts = List.of(contract1, contract2);

        SOWContract sowContract1 = createSOWContract(1, SOWContract.SOWContractStatus.Active, 1); // Assigned to user 1
        SOWContract sowContract2 = createSOWContract(2, SOWContract.SOWContractStatus.Active, 2); // Assigned to user 2
        List<SOWContract> allSOWContracts = List.of(sowContract1, sowContract2);

        ChangeRequest cr1 = createChangeRequest(1, "Approved", 1); // Linked to sowContract1
        List<ChangeRequest> allChangeRequests = List.of(cr1);

        // Mock getContacts - Sales Rep filters by assigneeUserId
        when(contactRepository.findAll()).thenReturn(allContacts);
        // Mock getOpportunities - Sales Rep filters by assigneeUserId
        when(opportunityRepository.findAll()).thenReturn(allOpportunities);
        // Mock getProposals - Sales Rep filters through opportunities
        when(proposalRepository.findAll()).thenReturn(allProposals);
        when(opportunityRepository.findById(1)).thenReturn(Optional.of(opp1));
        // Mock getMSAContracts - Sales Rep filters by assigneeUserId
        when(contractRepository.findAll()).thenReturn(allMSAContracts);
        // Mock getSOWContracts - Sales Rep filters by assigneeUserId
        when(sowContractRepository.findAll()).thenReturn(allSOWContracts);
        // Mock getChangeRequests - Sales Rep filters through contracts
        when(changeRequestRepository.findAll()).thenReturn(allChangeRequests);
        when(sowContractRepository.findById(1)).thenReturn(Optional.of(sowContract1));

        // Act
        SalesDashboardSummaryDTO result = salesDashboardService.getSummary(currentUser);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContacts().getAll()); // Only contact1 (assigned to user 1)
        assertEquals(1, result.getContacts().getNewCount());
        assertEquals(1, result.getOpportunities().getAll()); // Only opp1 (assigned to user 1)
        assertEquals(1, result.getProposals().getAll()); // Only proposal1 (linked to opp1)
        assertEquals(2, result.getContracts().getAll()); // contract1 + sowContract1 (both assigned to user 1)
        assertEquals(1, result.getChangeRequests().getAll()); // Only cr1 (linked to sowContract1)
        assertNull(result.getRevenue()); // Sales Rep không có revenue
    }

    @Test
    @DisplayName("getSummary - không có data → trả về zero counts")
    void testGetSummary_NoData() {
        // Arrange
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");

        when(contactRepository.findAll()).thenReturn(new ArrayList<>());
        when(opportunityRepository.findAll()).thenReturn(new ArrayList<>());
        when(proposalRepository.findAll()).thenReturn(new ArrayList<>());
        when(contractRepository.findAll()).thenReturn(new ArrayList<>());
        when(sowContractRepository.findAll()).thenReturn(new ArrayList<>());
        when(changeRequestRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        SalesDashboardSummaryDTO result = salesDashboardService.getSummary(currentUser);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContacts().getAll());
        assertEquals(0, result.getContacts().getNewCount());
        assertEquals(0, result.getOpportunities().getAll());
        assertEquals(0, result.getProposals().getAll());
        assertEquals(0, result.getContracts().getAll());
        assertEquals(0, result.getChangeRequests().getAll());
    }

    // Helper methods
    private User createUser(Integer id, String fullName, String email, String role) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private Contact createContact(Integer id, String status) {
        return createContact(id, status, null);
    }

    private Contact createContact(Integer id, String status, Integer assigneeUserId) {
        Contact contact = new Contact();
        contact.setId(id);
        contact.setStatus(status);
        contact.setAssigneeUserId(assigneeUserId);
        contact.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return contact;
    }

    private Opportunity createOpportunity(Integer id, String status) {
        return createOpportunity(id, status, null);
    }

    private Opportunity createOpportunity(Integer id, String status, Integer assigneeUserId) {
        Opportunity opportunity = new Opportunity();
        opportunity.setId(id);
        opportunity.setStatus(status);
        opportunity.setAssigneeUserId(assigneeUserId);
        opportunity.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return opportunity;
    }

    private Proposal createProposal(Integer id, String status) {
        return createProposal(id, status, null);
    }

    private Proposal createProposal(Integer id, String status, Integer createdBy) {
        Proposal proposal = new Proposal();
        proposal.setId(id);
        proposal.setStatus(status);
        proposal.setCreatedBy(createdBy);
        proposal.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return proposal;
    }

    private Contract createMSAContract(Integer id, Contract.ContractStatus status) {
        return createMSAContract(id, status, null);
    }

    private Contract createMSAContract(Integer id, Contract.ContractStatus status, Integer assigneeUserId) {
        Contract contract = new Contract();
        contract.setId(id);
        contract.setStatus(status);
        contract.setAssigneeUserId(assigneeUserId);
        contract.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return contract;
    }

    private SOWContract createSOWContract(Integer id, SOWContract.SOWContractStatus status) {
        return createSOWContract(id, status, null);
    }

    private SOWContract createSOWContract(Integer id, SOWContract.SOWContractStatus status, Integer assigneeUserId) {
        SOWContract contract = new SOWContract();
        contract.setId(id);
        contract.setStatus(status);
        contract.setAssigneeUserId(assigneeUserId);
        contract.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return contract;
    }

    private ChangeRequest createChangeRequest(Integer id, String status) {
        return createChangeRequest(id, status, null);
    }

    private ChangeRequest createChangeRequest(Integer id, String status, Integer sowContractId) {
        ChangeRequest cr = new ChangeRequest();
        cr.setId(id);
        cr.setStatus(status);
        cr.setSowContractId(sowContractId);
        cr.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return cr;
    }
}

