package com.skillbridge.controller.client.proposal;

import com.skillbridge.dto.proposal.response.ProposalListResponse;
import com.skillbridge.service.proposal.ProposalListService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientProposalControllerTest {

    @Mock
    private ProposalListService proposalListService;

    @InjectMocks
    private ClientProposalController controller;

    @Test
    @DisplayName("getProposals - userId null → dùng default 1 và gọi service")
    void testGetProposals_DefaultUserId() {
        ProposalListResponse res = new ProposalListResponse();
        when(proposalListService.getProposalsForClient(1, "q", "OPEN", 2, 50))
                .thenReturn(res);

        ResponseEntity<ProposalListResponse> response = controller.getProposals(
                "q", "OPEN", 2, 50, null
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(res, response.getBody());
        verify(proposalListService).getProposalsForClient(1, "q", "OPEN", 2, 50);
    }

    @Test
    @DisplayName("getProposals - exception → trả về emptyResponse 200")
    void testGetProposals_Exception() {
        when(proposalListService.getProposalsForClient(1, null, null, 0, 20))
                .thenThrow(new RuntimeException("err"));

        ResponseEntity<ProposalListResponse> response = controller.getProposals(
                null, null, 0, 20, null
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ProposalListResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getProposals());
        assertEquals(0, body.getProposals().size());
        assertEquals(0, body.getCurrentPage());
        assertEquals(0, body.getTotalPages());
        assertEquals(0, body.getTotalElements());
    }
}


