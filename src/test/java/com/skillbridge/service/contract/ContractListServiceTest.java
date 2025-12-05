package com.skillbridge.service.contract;

import com.skillbridge.dto.contract.response.ContractListResponse;
import com.skillbridge.entity.contract.Contract;
import com.skillbridge.entity.contract.SOWContract;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ContractListService
 * Tests CRUD operations for contract list
 */
@ExtendWith(MockitoExtension.class)
class ContractListServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private SOWContractRepository sowContractRepository;

    @InjectMocks
    private ContractListService contractListService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("getContracts - không có contract → trả về empty list")
    void testGetContracts_NoContracts() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        String type = "All";
        int page = 0;
        int size = 10;

        Page<Contract> emptyMSAPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 1000), 0);
        Page<SOWContract> emptySOWPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 1000), 0);

        when(contractRepository.findByClientIdWithFilters(anyInt(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyMSAPage);
        when(sowContractRepository.findByClientIdWithFilters(anyInt(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptySOWPage);

        // Act
        ContractListResponse response = contractListService.getContracts(
                clientUserId, search, status, type, page, size);

        // Assert
        assertNotNull(response);
        assertTrue(response.getContracts().isEmpty());
        assertEquals(0, response.getCurrentPage());
        assertEquals(0, response.getTotalPages());
        assertEquals(0, response.getTotalElements());

        verify(contractRepository).findByClientIdWithFilters(eq(clientUserId), isNull(), isNull(), any(Pageable.class));
        verify(sowContractRepository).findByClientIdWithFilters(eq(clientUserId), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("getContracts - có MSA contracts → trả về danh sách MSA")
    void testGetContracts_WithMSAContracts() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        String type = "MSA";
        int page = 0;
        int size = 10;

        Contract contract1 = createMSAContract(1, "Contract 1", Contract.ContractStatus.Active);
        Contract contract2 = createMSAContract(2, "Contract 2", Contract.ContractStatus.Pending);
        List<Contract> contracts = List.of(contract1, contract2);

        Page<Contract> msaPage = new PageImpl<>(contracts, PageRequest.of(0, 1000), 2);
        Page<SOWContract> emptySOWPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 1000), 0);

        when(contractRepository.findByClientIdWithFilters(anyInt(), any(), any(), any(Pageable.class)))
                .thenReturn(msaPage);
        when(sowContractRepository.findByClientIdWithFilters(anyInt(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptySOWPage);

        // Act
        ContractListResponse response = contractListService.getContracts(
                clientUserId, search, status, type, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getContracts().size());
        assertEquals(0, response.getCurrentPage());
        assertEquals(1, response.getTotalPages());
        assertEquals(2, response.getTotalElements());

        assertEquals("MSA", response.getContracts().get(0).getType());
        assertEquals("Contract 1", response.getContracts().get(0).getContractName());
        assertEquals("Active", response.getContracts().get(0).getStatus());

        verify(contractRepository).findByClientIdWithFilters(eq(clientUserId), isNull(), isNull(), any(Pageable.class));
        verify(sowContractRepository, never()).findByClientIdWithFilters(anyInt(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("getContracts - có SOW contracts → trả về danh sách SOW")
    void testGetContracts_WithSOWContracts() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        String type = "SOW";
        int page = 0;
        int size = 10;

        SOWContract sow1 = createSOWContract(1, "SOW Contract 1", SOWContract.SOWContractStatus.Active);
        SOWContract sow2 = createSOWContract(2, "SOW Contract 2", SOWContract.SOWContractStatus.Pending);
        List<SOWContract> sows = List.of(sow1, sow2);

        Page<Contract> emptyMSAPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 1000), 0);
        Page<SOWContract> sowPage = new PageImpl<>(sows, PageRequest.of(0, 1000), 2);

        when(contractRepository.findByClientIdWithFilters(anyInt(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyMSAPage);
        when(sowContractRepository.findByClientIdWithFilters(anyInt(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(sowPage);

        // Act
        ContractListResponse response = contractListService.getContracts(
                clientUserId, search, status, type, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getContracts().size());
        assertEquals(0, response.getCurrentPage());
        assertEquals(1, response.getTotalPages());
        assertEquals(2, response.getTotalElements());

        assertEquals("SOW", response.getContracts().get(0).getType());
        assertEquals("SOW Contract 1", response.getContracts().get(0).getContractName());
        assertEquals("Active", response.getContracts().get(0).getStatus());

        verify(contractRepository, never()).findByClientIdWithFilters(anyInt(), any(), any(), any(Pageable.class));
        verify(sowContractRepository).findByClientIdWithFilters(eq(clientUserId), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("getContracts - có search query → filter theo search")
    void testGetContracts_WithSearchQuery() {
        // Arrange
        Integer clientUserId = 5;
        String search = "Project";
        String status = "All";
        String type = "All";
        int page = 0;
        int size = 10;

        Contract contract = createMSAContract(1, "Project Contract", Contract.ContractStatus.Active);
        Page<Contract> msaPage = new PageImpl<>(List.of(contract), PageRequest.of(0, 1000), 1);
        Page<SOWContract> emptySOWPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 1000), 0);

        when(contractRepository.findByClientIdWithFilters(anyInt(), eq("Project"), any(), any(Pageable.class)))
                .thenReturn(msaPage);
        when(sowContractRepository.findByClientIdWithFilters(anyInt(), eq("Project"), any(), any(), any(Pageable.class)))
                .thenReturn(emptySOWPage);

        // Act
        ContractListResponse response = contractListService.getContracts(
                clientUserId, search, status, type, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContracts().size());
        verify(contractRepository).findByClientIdWithFilters(eq(clientUserId), eq("Project"), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("getContracts - có status filter → filter theo status")
    void testGetContracts_WithStatusFilter() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "Active";
        String type = "All";
        int page = 0;
        int size = 10;

        Contract contract = createMSAContract(1, "Active Contract", Contract.ContractStatus.Active);
        Page<Contract> msaPage = new PageImpl<>(List.of(contract), PageRequest.of(0, 1000), 1);
        Page<SOWContract> emptySOWPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 1000), 0);

        when(contractRepository.findByClientIdWithFilters(anyInt(), any(), eq(Contract.ContractStatus.Active), any(Pageable.class)))
                .thenReturn(msaPage);
        when(sowContractRepository.findByClientIdWithFilters(anyInt(), any(), eq(SOWContract.SOWContractStatus.Active), any(), any(Pageable.class)))
                .thenReturn(emptySOWPage);

        // Act
        ContractListResponse response = contractListService.getContracts(
                clientUserId, search, status, type, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContracts().size());
        verify(contractRepository).findByClientIdWithFilters(eq(clientUserId), isNull(), eq(Contract.ContractStatus.Active), any(Pageable.class));
    }

    @Test
    @DisplayName("getContracts - pagination → trả về đúng page")
    void testGetContracts_WithPagination() {
        // Arrange
        Integer clientUserId = 5;
        String search = null;
        String status = "All";
        String type = "All";
        int page = 1; // Page 1 (second page)
        int size = 2; // 2 items per page

        // Create 5 contracts
        List<Contract> allContracts = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            allContracts.add(createMSAContract(i, "Contract " + i, Contract.ContractStatus.Active));
        }

        Page<Contract> msaPage = new PageImpl<>(allContracts, PageRequest.of(0, 1000), 5);
        Page<SOWContract> emptySOWPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 1000), 0);

        when(contractRepository.findByClientIdWithFilters(anyInt(), any(), any(), any(Pageable.class)))
                .thenReturn(msaPage);
        when(sowContractRepository.findByClientIdWithFilters(anyInt(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptySOWPage);

        // Act
        ContractListResponse response = contractListService.getContracts(
                clientUserId, search, status, type, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getCurrentPage());
        assertEquals(3, response.getTotalPages()); // 5 items / 2 per page = 3 pages
        assertEquals(5, response.getTotalElements());
        assertEquals(2, response.getContracts().size()); // Page 1 should have 2 items (index 2, 3)
    }

    // Helper methods
    private Contract createMSAContract(Integer id, String name, Contract.ContractStatus status) {
        Contract contract = new Contract();
        contract.setId(id);
        contract.setClientId(5);
        contract.setContractName(name);
        contract.setStatus(status);
        contract.setPeriodStart(LocalDate.of(2025, 1, 1));
        contract.setPeriodEnd(LocalDate.of(2025, 12, 31));
        contract.setValue(new BigDecimal("1000000"));
        contract.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return contract;
    }

    private SOWContract createSOWContract(Integer id, String name, SOWContract.SOWContractStatus status) {
        SOWContract sow = new SOWContract();
        sow.setId(id);
        sow.setClientId(5);
        sow.setContractName(name);
        sow.setStatus(status);
        sow.setPeriodStart(LocalDate.of(2025, 1, 1));
        sow.setPeriodEnd(LocalDate.of(2025, 12, 31));
        sow.setValue(new BigDecimal("500000"));
        sow.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return sow;
    }
}

