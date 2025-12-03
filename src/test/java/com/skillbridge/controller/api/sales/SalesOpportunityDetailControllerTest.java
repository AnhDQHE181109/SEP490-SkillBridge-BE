package com.skillbridge.controller.api.sales;

import com.skillbridge.dto.sales.request.AssignReviewerRequest;
import com.skillbridge.dto.sales.request.CreateOpportunityRequest;
import com.skillbridge.dto.sales.request.SubmitReviewRequest;
import com.skillbridge.dto.sales.request.UpdateOpportunityRequest;
import com.skillbridge.dto.sales.response.ErrorResponse;
import com.skillbridge.dto.sales.response.OpportunityDetailDTO;
import com.skillbridge.dto.sales.response.ProposalDTO;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.sales.ProposalService;
import com.skillbridge.service.sales.SalesOpportunityDetailService;
import com.skillbridge.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOpportunityDetailControllerTest {

    @Mock
    private SalesOpportunityDetailService opportunityDetailService;

    @Mock
    private ProposalService proposalService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private SalesOpportunityDetailController controller;

    private User buildUser(String role) {
        User u = new User();
        u.setId(1);
        u.setRole(role);
        return u;
    }

    // ===== createFromContact =====

    @Test
    @DisplayName("createFromContact - currentUser null → 401")
    void testCreateFromContact_Unauthenticated() {
        when(authentication.getPrincipal()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<?> response = controller.createFromContact(
                1, new CreateOpportunityRequest(), authentication, httpServletRequest
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(opportunityDetailService);
    }

    @Test
    @DisplayName("createFromContact - role invalid → 403")
    void testCreateFromContact_ForbiddenRole() {
        User user = buildUser("ENGINEER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.createFromContact(
                1, new CreateOpportunityRequest(), authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(opportunityDetailService);
    }

    @Test
    @DisplayName("createFromContact - RuntimeException → 403 với ErrorResponse")
    void testCreateFromContact_RuntimeException() {
        User user = buildUser("SALES_REP");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        CreateOpportunityRequest req = new CreateOpportunityRequest();
        when(opportunityDetailService.createFromContact(1, req, user))
                .thenThrow(new RuntimeException("Contact not found"));

        ResponseEntity<?> response = controller.createFromContact(
                1, req, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertEquals("Contact not found", body.getMessage());
    }

    @Test
    @DisplayName("createFromContact - success → 200 với OpportunityDetailDTO")
    void testCreateFromContact_Success() {
        User user = buildUser("SALES_MANAGER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        CreateOpportunityRequest req = new CreateOpportunityRequest();
        OpportunityDetailDTO dto = new OpportunityDetailDTO();
        when(opportunityDetailService.createFromContact(1, req, user)).thenReturn(dto);

        ResponseEntity<?> response = controller.createFromContact(
                1, req, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    // ===== getOpportunityById =====

    @Test
    @DisplayName("getOpportunityById - currentUser null → 401")
    void testGetOpportunityById_Unauthenticated() {
        when(authentication.getPrincipal()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<?> response = controller.getOpportunityById(
                "OP-1", authentication, httpServletRequest
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(opportunityDetailService);
    }

    @Test
    @DisplayName("getOpportunityById - role invalid → 403")
    void testGetOpportunityById_ForbiddenRole() {
        User user = buildUser("ENGINEER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.getOpportunityById(
                "OP-1", authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(opportunityDetailService);
    }

    @Test
    @DisplayName("getOpportunityById - RuntimeException → 403 với ErrorResponse")
    void testGetOpportunityById_RuntimeException() {
        User user = buildUser("SALES_REP");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        when(opportunityDetailService.getOpportunityById("OP-1", user))
                .thenThrow(new RuntimeException("No permission"));

        ResponseEntity<?> response = controller.getOpportunityById(
                "OP-1", authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertEquals("No permission", body.getMessage());
    }

    @Test
    @DisplayName("getOpportunityById - success → 200 với OpportunityDetailDTO")
    void testGetOpportunityById_Success() {
        User user = buildUser("SALES_REP");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        OpportunityDetailDTO dto = new OpportunityDetailDTO();
        when(opportunityDetailService.getOpportunityById("OP-1", user)).thenReturn(dto);

        ResponseEntity<?> response = controller.getOpportunityById(
                "OP-1", authentication, httpServletRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    // ===== updateOpportunity =====

    @Test
    @DisplayName("updateOpportunity - currentUser null → 401")
    void testUpdateOpportunity_Unauthenticated() {
        when(authentication.getPrincipal()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<?> response = controller.updateOpportunity(
                "OP-1", new UpdateOpportunityRequest(), authentication, httpServletRequest
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(opportunityDetailService);
    }

    @Test
    @DisplayName("updateOpportunity - role invalid → 403")
    void testUpdateOpportunity_ForbiddenRole() {
        User user = buildUser("ENGINEER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.updateOpportunity(
                "OP-1", new UpdateOpportunityRequest(), authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(opportunityDetailService);
    }

    @Test
    @DisplayName("updateOpportunity - RuntimeException → 403 với ErrorResponse")
    void testUpdateOpportunity_RuntimeException() {
        User user = buildUser("SALES_MANAGER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        UpdateOpportunityRequest req = new UpdateOpportunityRequest();
        when(opportunityDetailService.updateOpportunity("OP-1", req, user))
                .thenThrow(new RuntimeException("Update not allowed"));

        ResponseEntity<?> response = controller.updateOpportunity(
                "OP-1", req, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertEquals("Update not allowed", body.getMessage());
    }

    @Test
    @DisplayName("updateOpportunity - success → 200 với OpportunityDetailDTO")
    void testUpdateOpportunity_Success() {
        User user = buildUser("SALES_MANAGER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        UpdateOpportunityRequest req = new UpdateOpportunityRequest();
        OpportunityDetailDTO dto = new OpportunityDetailDTO();
        when(opportunityDetailService.updateOpportunity("OP-1", req, user)).thenReturn(dto);

        ResponseEntity<?> response = controller.updateOpportunity(
                "OP-1", req, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    // ===== createProposal =====

    @Test
    @DisplayName("createProposal - currentUser null → 401")
    void testCreateProposal_Unauthenticated() {
        when(authentication.getPrincipal()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<?> response = controller.createProposal(
                "OP-1", "title", null, null, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(proposalService);
    }

    @Test
    @DisplayName("createProposal - role invalid → 403")
    void testCreateProposal_ForbiddenRole() {
        User user = buildUser("ENGINEER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.createProposal(
                "OP-1", "title", null, null, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(proposalService);
    }

    @Test
    @DisplayName("createProposal - RuntimeException → 403 với ErrorResponse")
    void testCreateProposal_RuntimeException() {
        User user = buildUser("SALES_REP");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        when(proposalService.createProposal(eq("OP-1"), eq("title"), isNull(), any(MultipartFile[].class), eq(user)))
                .thenThrow(new RuntimeException("Create failed"));

        ResponseEntity<?> response = controller.createProposal(
                "OP-1", "title", null, null, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertEquals("Create failed", body.getMessage());
    }

    @Test
    @DisplayName("createProposal - success → 200 với ProposalDTO")
    void testCreateProposal_Success() {
        User user = buildUser("SALES_REP");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ProposalDTO dto = new ProposalDTO();
        when(proposalService.createProposal(eq("OP-1"), eq("title"), isNull(), any(MultipartFile[].class), eq(user)))
                .thenReturn(dto);

        ResponseEntity<?> response = controller.createProposal(
                "OP-1", "title", null, null, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    // ===== updateProposal =====

    @Test
    @DisplayName("updateProposal - currentUser null → 401")
    void testUpdateProposal_Unauthenticated() {
        when(authentication.getPrincipal()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<?> response = controller.updateProposal(
                1, "title", null, null, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(proposalService);
    }

    @Test
    @DisplayName("updateProposal - role invalid → 403")
    void testUpdateProposal_ForbiddenRole() {
        User user = buildUser("ENGINEER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.updateProposal(
                1, "title", null, null, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(proposalService);
    }

    @Test
    @DisplayName("updateProposal - RuntimeException → 403 với ErrorResponse")
    void testUpdateProposal_RuntimeException() {
        User user = buildUser("SALES_MANAGER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        when(proposalService.updateProposal(eq(1), eq("title"), isNull(), any(MultipartFile[].class), eq(user)))
                .thenThrow(new RuntimeException("Update failed"));

        ResponseEntity<?> response = controller.updateProposal(
                1, "title", null, null, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertEquals("Update failed", body.getMessage());
    }

    @Test
    @DisplayName("updateProposal - success → 200 với ProposalDTO")
    void testUpdateProposal_Success() {
        User user = buildUser("SALES_MANAGER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ProposalDTO dto = new ProposalDTO();
        when(proposalService.updateProposal(eq(1), eq("title"), isNull(), any(MultipartFile[].class), eq(user)))
                .thenReturn(dto);

        ResponseEntity<?> response = controller.updateProposal(
                1, "title", null, null, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    // ===== assignReviewer =====

    @Test
    @DisplayName("assignReviewer - currentUser null → 401")
    void testAssignReviewer_Unauthenticated() {
        when(authentication.getPrincipal()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<?> response = controller.assignReviewer(
                1, new AssignReviewerRequest(), authentication, httpServletRequest
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(proposalService);
    }

    @Test
    @DisplayName("assignReviewer - role invalid → 403")
    void testAssignReviewer_ForbiddenRole() {
        User user = buildUser("ENGINEER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.assignReviewer(
                1, new AssignReviewerRequest(), authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(proposalService);
    }

    @Test
    @DisplayName("assignReviewer - RuntimeException → 403 với ErrorResponse")
    void testAssignReviewer_RuntimeException() {
        User user = buildUser("SALES_MANAGER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        AssignReviewerRequest req = new AssignReviewerRequest();
        req.setReviewerId(5);

        when(proposalService.assignReviewer(1, 5, user))
                .thenThrow(new RuntimeException("Reviewer invalid"));

        ResponseEntity<?> response = controller.assignReviewer(
                1, req, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertEquals("Reviewer invalid", body.getMessage());
    }

    @Test
    @DisplayName("assignReviewer - success → 200 với ProposalDTO")
    void testAssignReviewer_Success() {
        User user = buildUser("SALES_MANAGER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        AssignReviewerRequest req = new AssignReviewerRequest();
        req.setReviewerId(5);

        ProposalDTO dto = new ProposalDTO();
        when(proposalService.assignReviewer(1, 5, user)).thenReturn(dto);

        ResponseEntity<?> response = controller.assignReviewer(
                1, req, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    // ===== submitReview =====

    @Test
    @DisplayName("submitReview - currentUser null → 401")
    void testSubmitReview_Unauthenticated() {
        when(authentication.getPrincipal()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<?> response = controller.submitReview(
                1, new SubmitReviewRequest(), authentication, httpServletRequest
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(proposalService);
    }

    @Test
    @DisplayName("submitReview - role invalid → 403")
    void testSubmitReview_ForbiddenRole() {
        User user = buildUser("ENGINEER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.submitReview(
                1, new SubmitReviewRequest(), authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(proposalService);
    }

    @Test
    @DisplayName("submitReview - RuntimeException → 403 với ErrorResponse")
    void testSubmitReview_RuntimeException() {
        User user = buildUser("SALES_REP");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        SubmitReviewRequest req = new SubmitReviewRequest();
        when(proposalService.submitReview(1, req, user))
                .thenThrow(new RuntimeException("Submit failed"));

        ResponseEntity<?> response = controller.submitReview(
                1, req, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertEquals("Submit failed", body.getMessage());
    }

    @Test
    @DisplayName("submitReview - success → 200 với ProposalDTO")
    void testSubmitReview_Success() {
        User user = buildUser("SALES_REP");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        SubmitReviewRequest req = new SubmitReviewRequest();
        ProposalDTO dto = new ProposalDTO();
        when(proposalService.submitReview(1, req, user)).thenReturn(dto);

        ResponseEntity<?> response = controller.submitReview(
                1, req, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }
}


