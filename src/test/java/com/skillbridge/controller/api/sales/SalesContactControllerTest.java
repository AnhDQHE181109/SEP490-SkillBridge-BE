package com.skillbridge.controller.api.sales;

import com.skillbridge.dto.contact.response.CommunicationLogDTO;
import com.skillbridge.dto.sales.request.CreateCommunicationLogRequest;
import com.skillbridge.dto.sales.request.UpdateContactRequest;
import com.skillbridge.dto.sales.response.SalesContactDetailDTO;
import com.skillbridge.dto.sales.response.SalesContactListResponse;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.sales.SalesContactDetailService;
import com.skillbridge.service.sales.SalesContactService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesContactControllerTest {

    @Mock
    private SalesContactService salesContactService;

    @Mock
    private SalesContactDetailService salesContactDetailService;

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
    private SalesContactController controller;

    private User buildSalesUser(String role) {
        User u = new User();
        u.setId(1);
        u.setRole(role);
        return u;
    }

    @Test
    @DisplayName("getContacts - currentUser null → 401")
    void testGetContacts_Unauthenticated() {
        when(authentication.getPrincipal()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<SalesContactListResponse> response = controller.getContacts(
                null, null, null, 0, 20, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(salesContactService);
    }

    @Test
    @DisplayName("getContacts - role không phải SALES_MANAGER/SALES_REP → 403")
    void testGetContacts_ForbiddenRole() {
        User user = buildSalesUser("ENGINEER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity<SalesContactListResponse> response = controller.getContacts(
                null, null, null, 0, 20, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(salesContactService);
    }

    @Test
    @DisplayName("getContacts - SALES_MANAGER → gọi service và trả về 200")
    void testGetContacts_SalesManagerSuccess() {
        User user = buildSalesUser("SALES_MANAGER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        SalesContactListResponse serviceRes = new SalesContactListResponse();
        when(salesContactService.getContacts(
                eq("search"),
                eq(Arrays.asList("OPEN", "WON")),
                eq(5),
                eq(1),
                eq("SALES_MANAGER"),
                eq(2),
                eq(50)
        )).thenReturn(serviceRes);

        ResponseEntity<SalesContactListResponse> response = controller.getContacts(
                "search",
                Arrays.asList("OPEN", "WON"),
                5,
                2,
                50,
                authentication,
                httpServletRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceRes, response.getBody());
        verify(salesContactService).getContacts(
                "search",
                Arrays.asList("OPEN", "WON"),
                5,
                1,
                "SALES_MANAGER",
                2,
                50
        );
    }

    @Test
    @DisplayName("getContacts - fallback JWT token khi principal null")
    void testGetContacts_JwtFallback() {
        User user = buildSalesUser("SALES_REP");
        when(authentication.getPrincipal()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer jwt");
        when(jwtTokenProvider.isTokenExpired("jwt")).thenReturn(false);
        when(jwtTokenProvider.getUsernameFromToken("jwt")).thenReturn("rep@example.com");
        when(userRepository.findByEmail("rep@example.com")).thenReturn(Optional.of(user));

        SalesContactListResponse serviceRes = new SalesContactListResponse();
        when(salesContactService.getContacts(
                isNull(),
                eq(Collections.emptyList()),
                isNull(),
                eq(1),
                eq("SALES_REP"),
                eq(0),
                eq(20)
        )).thenReturn(serviceRes);

        ResponseEntity<SalesContactListResponse> response = controller.getContacts(
                null,
                Collections.emptyList(),
                null,
                0,
                20,
                authentication,
                httpServletRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceRes, response.getBody());
    }

    @Test
    @DisplayName("getContactDetail - currentUser null → 401")
    void testGetContactDetail_Unauthenticated() {
        when(authentication.getPrincipal()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<?> response = controller.getContactDetail(1, authentication, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(salesContactDetailService);
    }

    @Test
    @DisplayName("getContactDetail - role invalid → 403")
    void testGetContactDetail_ForbiddenRole() {
        User user = buildSalesUser("ENGINEER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.getContactDetail(1, authentication, httpServletRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(salesContactDetailService);
    }

    @Test
    @DisplayName("getContactDetail - RuntimeException từ service → 403 với ErrorResponse")
    void testGetContactDetail_RuntimeException() {
        User user = buildSalesUser("SALES_REP");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        when(salesContactDetailService.getContactDetail(eq(10), eq(user)))
                .thenThrow(new RuntimeException("No permission"));

        ResponseEntity<?> response = controller.getContactDetail(10, authentication, httpServletRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof SalesContactController.ErrorResponse);
        SalesContactController.ErrorResponse body =
                (SalesContactController.ErrorResponse) response.getBody();
        assertEquals("No permission", body.getMessage());
    }

    @Test
    @DisplayName("getContactDetail - success → 200 với SalesContactDetailDTO")
    void testGetContactDetail_Success() {
        User user = buildSalesUser("SALES_REP");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        SalesContactDetailDTO dto = new SalesContactDetailDTO();
        when(salesContactDetailService.getContactDetail(10, user)).thenReturn(dto);

        ResponseEntity<?> response = controller.getContactDetail(10, authentication, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("updateContact - currentUser null → 401")
    void testUpdateContact_Unauthenticated() {
        when(authentication.getPrincipal()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<?> response = controller.updateContact(1, new UpdateContactRequest(), authentication, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(salesContactDetailService);
    }

    @Test
    @DisplayName("updateContact - role invalid → 403")
    void testUpdateContact_ForbiddenRole() {
        User user = buildSalesUser("ENGINEER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.updateContact(1, new UpdateContactRequest(), authentication, httpServletRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(salesContactDetailService);
    }

    @Test
    @DisplayName("updateContact - RuntimeException → 403 với ErrorResponse")
    void testUpdateContact_RuntimeException() {
        User user = buildSalesUser("SALES_MANAGER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        UpdateContactRequest req = new UpdateContactRequest();
        when(salesContactDetailService.updateContact(eq(5), eq(req), eq(user)))
                .thenThrow(new RuntimeException("Update not allowed"));

        ResponseEntity<?> response = controller.updateContact(5, req, authentication, httpServletRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof SalesContactController.ErrorResponse);
        SalesContactController.ErrorResponse body =
                (SalesContactController.ErrorResponse) response.getBody();
        assertEquals("Update not allowed", body.getMessage());
    }

    @Test
    @DisplayName("updateContact - success → 200 với SalesContactDetailDTO")
    void testUpdateContact_Success() {
        User user = buildSalesUser("SALES_MANAGER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        UpdateContactRequest req = new UpdateContactRequest();
        SalesContactDetailDTO dto = new SalesContactDetailDTO();
        when(salesContactDetailService.updateContact(5, req, user)).thenReturn(dto);

        ResponseEntity<?> response = controller.updateContact(5, req, authentication, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("convertToOpportunity - currentUser null → 401")
    void testConvertToOpportunity_Unauthenticated() {
        when(authentication.getPrincipal()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<?> response = controller.convertToOpportunity(1, authentication, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(salesContactDetailService);
    }

    @Test
    @DisplayName("convertToOpportunity - role invalid → 403")
    void testConvertToOpportunity_ForbiddenRole() {
        User user = buildSalesUser("ENGINEER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.convertToOpportunity(1, authentication, httpServletRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(salesContactDetailService);
    }

    @Test
    @DisplayName("convertToOpportunity - RuntimeException → 403 với ErrorResponse")
    void testConvertToOpportunity_RuntimeException() {
        User user = buildSalesUser("SALES_REP");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        when(salesContactDetailService.convertToOpportunity(7, user))
                .thenThrow(new RuntimeException("Already converted"));

        ResponseEntity<?> response = controller.convertToOpportunity(7, authentication, httpServletRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof SalesContactController.ErrorResponse);
        SalesContactController.ErrorResponse body =
                (SalesContactController.ErrorResponse) response.getBody();
        assertEquals("Already converted", body.getMessage());
    }

    @Test
    @DisplayName("convertToOpportunity - success → 200 với SalesContactDetailDTO")
    void testConvertToOpportunity_Success() {
        User user = buildSalesUser("SALES_REP");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        SalesContactDetailDTO dto = new SalesContactDetailDTO();
        when(salesContactDetailService.convertToOpportunity(7, user)).thenReturn(dto);

        ResponseEntity<?> response = controller.convertToOpportunity(7, authentication, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("addCommunicationLog - currentUser null → 401")
    void testAddCommunicationLog_Unauthenticated() {
        when(authentication.getPrincipal()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<?> response = controller.addCommunicationLog(
                1, new CreateCommunicationLogRequest(), authentication, httpServletRequest
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(salesContactDetailService);
    }

    @Test
    @DisplayName("addCommunicationLog - role invalid → 403")
    void testAddCommunicationLog_ForbiddenRole() {
        User user = buildSalesUser("ENGINEER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.addCommunicationLog(
                1, new CreateCommunicationLogRequest(), authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(salesContactDetailService);
    }

    @Test
    @DisplayName("addCommunicationLog - RuntimeException → 403 với ErrorResponse")
    void testAddCommunicationLog_RuntimeException() {
        User user = buildSalesUser("SALES_MANAGER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        CreateCommunicationLogRequest req = new CreateCommunicationLogRequest();
        when(salesContactDetailService.addCommunicationLog(3, req, user))
                .thenThrow(new RuntimeException("Contact not found"));

        ResponseEntity<?> response = controller.addCommunicationLog(
                3, req, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof SalesContactController.ErrorResponse);
        SalesContactController.ErrorResponse body =
                (SalesContactController.ErrorResponse) response.getBody();
        assertEquals("Contact not found", body.getMessage());
    }

    @Test
    @DisplayName("addCommunicationLog - success → 200 với CommunicationLogDTO")
    void testAddCommunicationLog_Success() {
        User user = buildSalesUser("SALES_MANAGER");
        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        CreateCommunicationLogRequest req = new CreateCommunicationLogRequest();
        CommunicationLogDTO dto = new CommunicationLogDTO();
        when(salesContactDetailService.addCommunicationLog(3, req, user)).thenReturn(dto);

        ResponseEntity<?> response = controller.addCommunicationLog(
                3, req, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }
}


