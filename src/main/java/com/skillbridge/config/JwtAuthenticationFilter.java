package com.skillbridge.config;

import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.util.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * JWT Authentication Filter
 * Intercepts requests and validates JWT tokens from Authorization header
 * Sets authentication in SecurityContext if token is valid
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // Extract JWT token from Authorization header
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                // Validate token and extract email
                // Check if token is valid and not expired
                if (token != null && !token.isEmpty()) {
                    try {
                        // Check if token is expired first (this will throw if token is invalid)
                        if (!jwtTokenProvider.isTokenExpired(token)) {
                            String email = jwtTokenProvider.getUsernameFromToken(token);
                            
                            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                                // Load user from database
                                Optional<User> userOpt = userRepository.findByEmail(email);
                                
                                if (userOpt.isPresent()) {
                                    User user = userOpt.get();
                                    
                                    // Check if user is active
                                    if (user.getIsActive() != null && user.getIsActive()) {
                                        // Validate token with user
                                        if (jwtTokenProvider.validateToken(token, user)) {
                                            // Create authentication token with user email and role
                                            String role = user.getRole() != null ? user.getRole() : "CLIENT";
                                            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                                            
                                            UsernamePasswordAuthenticationToken authentication = 
                                                new UsernamePasswordAuthenticationToken(
                                                    email, // principal
                                                    null,  // credentials (not needed)
                                                    Collections.singletonList(authority)
                                                );
                                            
                                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                            
                                            // Set authentication in SecurityContext
                                            SecurityContextHolder.getContext().setAuthentication(authentication);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception tokenException) {
                        // Token is invalid, expired, or malformed - log and continue
                        logger.debug("JWT token validation failed: " + tokenException.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            // Log error but continue filter chain
            // Token is invalid or expired - request will be rejected by Spring Security
            logger.debug("JWT authentication filter error: " + e.getMessage());
        }
        
        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}

