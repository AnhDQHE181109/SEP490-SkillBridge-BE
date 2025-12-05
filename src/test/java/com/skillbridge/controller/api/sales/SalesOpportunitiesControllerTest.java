package com.skillbridge.controller.api.sales;

import com.skillbridge.dto.sales.response.OpportunitiesListResponse;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.sales.SalesOpportunitiesService;
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
class SalesOpportunitiesControllerTest {

    @Mock
    private SalesOpportunitiesService salesOpportunitiesService;

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
    private SalesOpportunitiesController controller;

    @Test
    @DisplayName("getOpportunities - currentUser null → 401")
    void testGetOpportunities_Unauthenticated() {
        when(authentication.isAuthenticated()).thenReturn(false);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<OpportunitiesListResponse> response = controller.getOpportunities(
                null, null, null, null, 0, 20, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(salesOpportunitiesService);
    }

    @Test
    @DisplayName("getOpportunities - role not SALES_MANAGER/SALES_REP → 403")
    void testGetOpportunities_ForbiddenRole() {
        User user = new User();
        user.setId(1);
        user.setRole("ENGINEER");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        ResponseEntity<OpportunitiesListResponse> response = controller.getOpportunities(
                null, null, null, null, 0, 20, authentication, httpServletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(salesOpportunitiesService);
    }

    @Test
    @DisplayName("getOpportunities - authenticated + SALES_MANAGER → gọi service và trả về 200")
    void testGetOpportunities_SalesManagerSuccess() {
        User user = new User();
        user.setId(10);
        user.setRole("SALES_MANAGER");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("manager@example.com");
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(user));

        OpportunitiesListResponse serviceRes = new OpportunitiesListResponse();
        when(salesOpportunitiesService.getOpportunities(
                eq("search-key"),
                eq(Arrays.asList("OPEN", "WON")),
                eq(5),
                eq(10),
                eq(10),
                eq("SALES_MANAGER"),
                eq(0),
                eq(50)
        )).thenReturn(serviceRes);

        ResponseEntity<OpportunitiesListResponse> response = controller.getOpportunities(
                "search-key",
                Arrays.asList("OPEN", "WON"),
                5,
                10,
                0,
                50,
                authentication,
                httpServletRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceRes, response.getBody());
        verify(salesOpportunitiesService).getOpportunities(
                "search-key",
                Arrays.asList("OPEN", "WON"),
                5,
                10,
                10,
                "SALES_MANAGER",
                0,
                50
        );
    }

    @Test
    @DisplayName("getOpportunities - fallback JWT token khi authentication null")
    void testGetOpportunities_UsingJwtFallback() {
        User user = new User();
        user.setId(20);
        user.setRole("SALES_REP");

        when(jwtTokenProvider.getEmailFromToken("jwt-token")).thenReturn("rep@example.com");
        when(userRepository.findByEmail("rep@example.com")).thenReturn(Optional.of(user));
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer jwt-token");

        OpportunitiesListResponse serviceRes = new OpportunitiesListResponse();
        when(salesOpportunitiesService.getOpportunities(
                eq(null),
                eq(Collections.emptyList()),
                isNull(),
                isNull(),
                eq(20),
                eq("SALES_REP"),
                eq(1),
                eq(10)
        )).thenReturn(serviceRes);

        ResponseEntity<OpportunitiesListResponse> response = controller.getOpportunities(
                null,
                Collections.emptyList(),
                null,
                null,
                1,
                10,
                null,
                httpServletRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceRes, response.getBody());
    }
}


