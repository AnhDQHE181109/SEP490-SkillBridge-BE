package com.skillbridge.service.sales;

import com.skillbridge.dto.contract.response.ContractListResponse;
import com.skillbridge.dto.contract.response.ContractListItemDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contract.Contract;
import com.skillbridge.entity.contract.SOWContract;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contract.ContractRepository;
import com.skillbridge.repository.contract.SOWContractRepository;
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
 * Unit tests for SalesContractService
 * Tests business logic for sales contract list operations with role-based filtering
 */
@ExtendWith(MockitoExtension.class)
class SalesContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private SOWContractRepository sowContractRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SalesContractService salesContractService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("getContracts - Sales Manager không có filter → trả về tất cả contracts")
    void testGetContracts_SalesManager_NoFilter() {
        // Arrange
        String search = null;
        String status = null;
        int page = 0;
        int size = 20;
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");

        Contract msaContract = createMSAContract(1, Contract.ContractStatus.Active);
        SOWContract sowContract = createSOWContract(1, SOWContract.SOWContractStatus.Active);
        List<Contract> msaContracts = List.of(msaContract);
        List<SOWContract> sowContracts = List.of(sowContract);

        Page<Contract> msaPage = new PageImpl<>(msaContracts, PageRequest.of(0, 10000), 1);
        Page<SOWContract> sowPage = new PageImpl<>(sowContracts, PageRequest.of(0, 10000), 1);

        when(contractRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(msaPage);
        when(sowContractRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(sowPage);
        when(userRepository.findById(anyInt())).thenReturn(Optional.of(createUser(10, "Client", "client@example.com", "CLIENT")));

        // Act
        ContractListResponse response = salesContractService.getContracts(search, status, page, size, currentUser);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getContracts().size()); // MSA + SOW
        assertEquals(0, response.getCurrentPage());
        assertEquals(1, response.getTotalPages());
        assertEquals(2, response.getTotalElements());

        verify(contractRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(sowContractRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getContracts - Sales Rep → chỉ trả về contracts được assign")
    void testGetContracts_SalesRep_OnlyAssigned() {
        // Arrange
        String search = null;
        String status = null;
        int page = 0;
        int size = 20;
        User currentUser = createUser(1, "Sales Rep", "salesrep@example.com", "SALES_REP");

        Contract msaContract = createMSAContract(1, Contract.ContractStatus.Active, 1); // Assigned to user 1
        Contract msaContract2 = createMSAContract(2, Contract.ContractStatus.Active, 2); // Assigned to user 2
        SOWContract sowContract = createSOWContract(1, SOWContract.SOWContractStatus.Active, 1); // Assigned to user 1
        List<Contract> allMSAContracts = List.of(msaContract, msaContract2);
        List<SOWContract> allSOWContracts = List.of(sowContract);

        Page<Contract> msaPage = new PageImpl<>(allMSAContracts, PageRequest.of(0, 10000), 2);
        Page<SOWContract> sowPage = new PageImpl<>(allSOWContracts, PageRequest.of(0, 10000), 1);

        when(contractRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(msaPage);
        when(sowContractRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(sowPage);
        when(userRepository.findById(anyInt())).thenReturn(Optional.of(createUser(10, "Client", "client@example.com", "CLIENT")));

        // Act
        ContractListResponse response = salesContractService.getContracts(search, status, page, size, currentUser);

        // Assert
        assertNotNull(response);
        // Should filter to only contracts assigned to user 1
        assertEquals(2, response.getContracts().size()); // msaContract + sowContract (both assigned to user 1)

        verify(contractRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(sowContractRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getContracts - có search query → filter theo search")
    void testGetContracts_WithSearchQuery() {
        // Arrange
        String search = "Client";
        String status = null;
        int page = 0;
        int size = 20;
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");

        Contract msaContract = createMSAContract(1, Contract.ContractStatus.Active);
        SOWContract sowContract = createSOWContract(1, SOWContract.SOWContractStatus.Active);
        List<Contract> msaContracts = List.of(msaContract);
        List<SOWContract> sowContracts = List.of(sowContract);

        Page<Contract> msaPage = new PageImpl<>(msaContracts, PageRequest.of(0, 10000), 1);
        Page<SOWContract> sowPage = new PageImpl<>(sowContracts, PageRequest.of(0, 10000), 1);

        when(contractRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(msaPage);
        when(sowContractRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(sowPage);
        when(userRepository.findById(anyInt())).thenReturn(Optional.of(createUser(10, "Client Name", "client@example.com", "CLIENT")));

        // Act
        ContractListResponse response = salesContractService.getContracts(search, status, page, size, currentUser);

        // Assert
        assertNotNull(response);
        // Search should filter by client name/email
        verify(contractRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(sowContractRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getContracts - có status filter → filter theo status")
    void testGetContracts_WithStatusFilter() {
        // Arrange
        String search = null;
        String status = "Active";
        int page = 0;
        int size = 20;
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");

        Contract msaContract = createMSAContract(1, Contract.ContractStatus.Active);
        SOWContract sowContract = createSOWContract(1, SOWContract.SOWContractStatus.Active);
        List<Contract> msaContracts = List.of(msaContract);
        List<SOWContract> sowContracts = List.of(sowContract);

        Page<Contract> msaPage = new PageImpl<>(msaContracts, PageRequest.of(0, 10000), 1);
        Page<SOWContract> sowPage = new PageImpl<>(sowContracts, PageRequest.of(0, 10000), 1);

        when(contractRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(msaPage);
        when(sowContractRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(sowPage);
        when(userRepository.findById(anyInt())).thenReturn(Optional.of(createUser(10, "Client", "client@example.com", "CLIENT")));

        // Act
        ContractListResponse response = salesContractService.getContracts(search, status, page, size, currentUser);

        // Assert
        assertNotNull(response);
        verify(contractRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(sowContractRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getContracts - pagination → trả về đúng page")
    void testGetContracts_WithPagination() {
        // Arrange
        String search = null;
        String status = null;
        int page = 1; // Page 1
        int size = 2; // 2 items per page
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");

        Contract msaContract1 = createMSAContract(1, Contract.ContractStatus.Active);
        Contract msaContract2 = createMSAContract(2, Contract.ContractStatus.Active);
        Contract msaContract3 = createMSAContract(3, Contract.ContractStatus.Active);
        SOWContract sowContract1 = createSOWContract(1, SOWContract.SOWContractStatus.Active);
        List<Contract> msaContracts = List.of(msaContract1, msaContract2, msaContract3);
        List<SOWContract> sowContracts = List.of(sowContract1);

        Page<Contract> msaPage = new PageImpl<>(msaContracts, PageRequest.of(0, 10000), 3);
        Page<SOWContract> sowPage = new PageImpl<>(sowContracts, PageRequest.of(0, 10000), 1);

        when(contractRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(msaPage);
        when(sowContractRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(sowPage);
        when(userRepository.findById(anyInt())).thenReturn(Optional.of(createUser(10, "Client", "client@example.com", "CLIENT")));

        // Act
        ContractListResponse response = salesContractService.getContracts(search, status, page, size, currentUser);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getCurrentPage());
        // Total 4 contracts (3 MSA + 1 SOW), 2 per page = 2 pages
        assertEquals(2, response.getTotalPages());
        assertEquals(4, response.getTotalElements());
        assertEquals(2, response.getContracts().size()); // Page 1 should have 2 items
    }

    @Test
    @DisplayName("getContracts - không có contracts → trả về empty list")
    void testGetContracts_NoContracts() {
        // Arrange
        String search = null;
        String status = null;
        int page = 0;
        int size = 20;
        User currentUser = createUser(1, "Sales Manager", "manager@example.com", "SALES_MANAGER");

        Page<Contract> emptyMSAPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10000), 0);
        Page<SOWContract> emptySOWPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10000), 0);

        when(contractRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyMSAPage);
        when(sowContractRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptySOWPage);

        // Act
        ContractListResponse response = salesContractService.getContracts(search, status, page, size, currentUser);

        // Assert
        assertNotNull(response);
        assertTrue(response.getContracts().isEmpty());
        assertEquals(0, response.getTotalElements());
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

    private Contract createMSAContract(Integer id, Contract.ContractStatus status) {
        return createMSAContract(id, status, null);
    }

    private Contract createMSAContract(Integer id, Contract.ContractStatus status, Integer assigneeUserId) {
        Contract contract = new Contract();
        contract.setId(id);
        contract.setStatus(status);
        contract.setAssigneeUserId(assigneeUserId);
        contract.setClientId(10);
        contract.setContractName("Test MSA Contract");
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
        contract.setClientId(10);
        contract.setContractName("Test SOW Contract");
        contract.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return contract;
    }
}

