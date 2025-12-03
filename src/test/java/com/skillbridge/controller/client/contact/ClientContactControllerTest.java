package com.skillbridge.controller.client.contact;

import com.skillbridge.dto.contact.request.*;
import com.skillbridge.dto.contact.response.*;
import com.skillbridge.service.contact.ContactDetailService;
import com.skillbridge.service.contact.ContactListService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientContactControllerTest {

    @Mock
    private ContactListService contactListService;

    @Mock
    private ContactDetailService contactDetailService;

    @InjectMocks
    private ClientContactController controller;

    @Test
    @DisplayName("getContacts - userId null → dùng default 1 và gọi service")
    void testGetContacts_DefaultUserId() {
        ContactListResponse res = new ContactListResponse();
        when(contactListService.getContactsForClient(1, "q", "OPEN", 2, 50))
                .thenReturn(res);

        ResponseEntity<ContactListResponse> response = controller.getContacts(
                "q", "OPEN", 2, 50, null
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(res, response.getBody());
        verify(contactListService).getContactsForClient(1, "q", "OPEN", 2, 50);
    }

    @Test
    @DisplayName("createContact - success true → 200")
    void testCreateContact_Success() {
        CreateContactRequest req = new CreateContactRequest();
        CreateContactResponse res = new CreateContactResponse();
        res.setSuccess(true);

        when(contactListService.createContact(5, req)).thenReturn(res);

        ResponseEntity<CreateContactResponse> response = controller.createContact(req, 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(res, response.getBody());
    }

    @Test
    @DisplayName("createContact - success false → 400")
    void testCreateContact_Failure() {
        CreateContactRequest req = new CreateContactRequest();
        CreateContactResponse res = new CreateContactResponse();
        res.setSuccess(false);

        when(contactListService.createContact(1, req)).thenReturn(res);

        ResponseEntity<CreateContactResponse> response = controller.createContact(req, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertSame(res, response.getBody());
    }

    @Test
    @DisplayName("getContactDetail - dùng default userId 1 khi null")
    void testGetContactDetail_DefaultUserId() {
        ContactDetailDTO dto = new ContactDetailDTO();
        when(contactDetailService.getContactDetail(10, 1)).thenReturn(dto);

        ResponseEntity<ContactDetailDTO> response = controller.getContactDetail(10, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
        verify(contactDetailService).getContactDetail(10, 1);
    }

    @Test
    @DisplayName("addCommunicationLog - dùng default userId và gọi service")
    void testAddCommunicationLog_DefaultUserId() {
        AddLogRequest req = new AddLogRequest();
        req.setMessage("hello");
        CommunicationLogDTO dto = new CommunicationLogDTO();

        when(contactDetailService.addCommunicationLog(3, 1, "hello")).thenReturn(dto);

        ResponseEntity<CommunicationLogDTO> response = controller.addCommunicationLog(3, req, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("addProposalComment - dùng default userId và gọi service")
    void testAddProposalComment_DefaultUserId() {
        CommentRequest req = new CommentRequest();
        req.setMessage("cmt");
        CommentResponse dto = new CommentResponse();

        when(contactDetailService.addProposalComment(4, 1, "cmt")).thenReturn(dto);

        ResponseEntity<CommentResponse> response = controller.addProposalComment(4, req, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("cancelConsultation - dùng default userId và gọi service")
    void testCancelConsultation_DefaultUserId() {
        CancelRequest req = new CancelRequest();
        req.setReason("no need");
        CancelResponse dto = new CancelResponse();

        when(contactDetailService.cancelConsultation(7, 1, "no need")).thenReturn(dto);

        ResponseEntity<CancelResponse> response = controller.cancelConsultation(7, req, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("approveProposal - dùng default userId và gọi service")
    void testApproveProposal_DefaultUserId() {
        ApproveResponse dto = new ApproveResponse();
        when(contactDetailService.approveProposal(8, 1)).thenReturn(dto);

        ResponseEntity<ApproveResponse> response = controller.approveProposal(8, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }
}


