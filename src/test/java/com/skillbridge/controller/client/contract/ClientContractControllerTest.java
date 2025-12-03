package com.skillbridge.controller.client.contract;

import com.skillbridge.dto.contract.response.ContractDetailDTO;
import com.skillbridge.dto.contract.response.ContractListResponse;
import com.skillbridge.service.contract.ContractDetailService;
import com.skillbridge.service.contract.ContractListService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientContractControllerTest {

    @Mock
    private ContractListService contractListService;

    @Mock
    private ContractDetailService contractDetailService;

    @InjectMocks
    private ClientContractController controller;

    @Test
    @DisplayName("getContracts - userId null → default 1 và gọi service")
    void testGetContracts_DefaultUserId() {
        ContractListResponse res = new ContractListResponse();
        when(contractListService.getContracts(1, "q", "ACTIVE", "MSA", 1, 20))
                .thenReturn(res);

        ResponseEntity<?> response = controller.getContracts(
                "q", "ACTIVE", "MSA", 1, 20, null
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(res, response.getBody());
    }

    @Test
    @DisplayName("getContracts - exception → 500 với ErrorResponse")
    void testGetContracts_Exception() {
        when(contractListService.getContracts(anyInt(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.getContracts(
                null, null, null, 0, 20, 5
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("getContractDetail - EntityNotFound → 404")
    void testGetContractDetail_NotFound() {
        when(contractDetailService.getContractDetail(10, 1))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("not found"));

        ResponseEntity<?> response = controller.getContractDetail(10, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("getContractDetail - success → 200 với ContractDetailDTO")
    void testGetContractDetail_Success() {
        ContractDetailDTO dto = new ContractDetailDTO();
        when(contractDetailService.getContractDetail(10, 5)).thenReturn(dto);

        ResponseEntity<?> response = controller.getContractDetail(10, 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("getSOWContractVersions - EntityNotFound → 404")
    void testGetVersions_NotFound() {
        when(contractDetailService.getSOWContractVersions(10, 1))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("not found"));

        ResponseEntity<?> response = controller.getSOWContractVersions(10, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("getSOWContractVersions - success → 200 với list")
    void testGetVersions_Success() {
        List<ContractDetailDTO> list = Collections.singletonList(new ContractDetailDTO());
        when(contractDetailService.getSOWContractVersions(10, 2)).thenReturn(list);

        ResponseEntity<?> response = controller.getSOWContractVersions(10, 2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(list, response.getBody());
    }

    @Test
    @DisplayName("approveContract - EntityNotFound → 404")
    void testApproveContract_NotFound() {
        doThrow(new jakarta.persistence.EntityNotFoundException("not found"))
                .when(contractDetailService).approveContract(10, 1);

        ResponseEntity<?> response = controller.approveContract(10, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("approveContract - IllegalStateException → 400")
    void testApproveContract_IllegalState() {
        doThrow(new IllegalStateException("cannot"))
                .when(contractDetailService).approveContract(10, 1);

        ResponseEntity<?> response = controller.approveContract(10, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("approveContract - success → 200 với message map")
    void testApproveContract_Success() {
        ResponseEntity<?> response = controller.approveContract(10, 3);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Contract approved successfully", body.get("message"));
        assertEquals(10, body.get("contractId"));
    }

    @Test
    @DisplayName("addComment - missing comment → 400")
    void testAddComment_MissingComment() {
        Map<String, String> req = new HashMap<>();

        ResponseEntity<?> response = controller.addComment(10, req, 1);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        verifyNoInteractions(contractDetailService);
    }

    @Test
    @DisplayName("addComment - success → 200 và gọi service")
    void testAddComment_Success() {
        Map<String, String> req = new HashMap<>();
        req.put("comment", "nice");

        ResponseEntity<?> response = controller.addComment(10, req, 2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        verify(contractDetailService).addComment(10, 2, "nice");
    }

    @Test
    @DisplayName("cancelContract - success → 200 và gọi service")
    void testCancelContract_Success() {
        Map<String, String> req = new HashMap<>();
        req.put("reason", "no longer needed");

        ResponseEntity<?> response = controller.cancelContract(10, req, 3);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        verify(contractDetailService).cancelContract(10, 3, "no longer needed");
    }
}


