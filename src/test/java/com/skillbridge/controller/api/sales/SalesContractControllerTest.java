package com.skillbridge.controller.api.sales;

import com.skillbridge.dto.contract.response.ContractListResponse;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.sales.SalesContractService;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesContractControllerTest {

    @Mock
    private SalesContractService contractService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private SalesContractController controller;

    @Test
    @DisplayName("getContracts - currentUser null → 401")
    void testGetContracts_Unauthenticated() {
        when(authentication.getPrincipal()).thenReturn(null);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<?> response = controller.getContracts(
                null, null, 0, 20, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(contractService);
    }

    @Test
    @DisplayName("getContracts - role not SALES_MANAGER/SALES_REP → 403")
    void testGetContracts_ForbiddenRole() {
        User user = new User();
        user.setId(1);
        user.setRole("ENGINEER");

        when(authentication.getPrincipal()).thenReturn("1");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.getContracts(
                null, null, 0, 20, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(contractService);
    }

    @Test
    @DisplayName("getContracts - SALES_MANAGER → gọi service và trả về 200")
    void testGetContracts_SalesManagerSuccess() {
        User user = new User();
        user.setId(10);
        user.setRole("SALES_MANAGER");

        when(authentication.getPrincipal()).thenReturn("10");
        when(userRepository.findById(10)).thenReturn(Optional.of(user));

        ContractListResponse serviceRes = new ContractListResponse();
        when(contractService.getContracts(
                eq("search"), eq("ACTIVE"), eq(1), eq(50), eq(user)
        )).thenReturn(serviceRes);

        ResponseEntity<?> response = controller.getContracts(
                "search", "ACTIVE", 1, 50, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceRes, response.getBody());
        verify(contractService).getContracts("search", "ACTIVE", 1, 50, user);
    }

    @Test
    @DisplayName("getContracts - RuntimeException từ service → 403 với ErrorResponse")
    void testGetContracts_RuntimeException() {
        User user = new User();
        user.setId(10);
        user.setRole("SALES_REP");

        when(authentication.getPrincipal()).thenReturn("10");
        when(userRepository.findById(10)).thenReturn(Optional.of(user));

        when(contractService.getContracts(anyString(), anyString(), anyInt(), anyInt(), any(User.class)))
                .thenThrow(new RuntimeException("No permission"));

        ResponseEntity<?> response = controller.getContracts(
            "search", "ACTIVE", 0, 20, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof SalesContractController.ErrorResponse);
        SalesContractController.ErrorResponse body =
                (SalesContractController.ErrorResponse) response.getBody();
        assertEquals("No permission", body.getMessage());
    }
}


