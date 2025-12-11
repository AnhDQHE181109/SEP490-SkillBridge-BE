package com.skillbridge.controller.api.sales;

import com.skillbridge.dto.contract.request.CreateProjectCloseRequestRequest;
import com.skillbridge.dto.contract.request.ResubmitProjectCloseRequestRequest;
import com.skillbridge.dto.contract.response.ProjectCloseRequestResponse;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.service.contract.ProjectCloseRequestService;
import com.skillbridge.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Sales Project Close Request Controller
 * Handles project close request endpoints for Sales Portal
 * Story-41: Project Close Request for SOW Contract
 */
@RestController
@RequestMapping("/sales/sows/{sowId}/close-requests")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:4200"}, 
             allowCredentials = "true",
             maxAge = 3600)
public class SalesProjectCloseRequestController {

    @Autowired
    private ProjectCloseRequestService projectCloseRequestService;

    /**
     * Create a new Project Close Request
     * POST /sales/sows/{sowId}/close-requests
     */
    @PostMapping
    public ResponseEntity<?> createCloseRequest(
            @PathVariable Integer sowId,
            @RequestBody CreateProjectCloseRequestRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        try {
            User currentUser = getCurrentUser(authentication, httpRequest);
            if (currentUser == null) {
                return ResponseEntity.status(401).body(new ErrorResponse("User not authenticated"));
            }

            String role = currentUser.getRole();
            if (role == null || (!role.equals("SALES_MANAGER") && !role.equals("SALES_REP"))) {
                return ResponseEntity.status(403).body(new ErrorResponse("Access denied: Only Sales Representatives and Sales Managers can create close requests"));
            }

            ProjectCloseRequestResponse response = projectCloseRequestService.createCloseRequest(
                    sowId, request, currentUser
            );

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }
}

