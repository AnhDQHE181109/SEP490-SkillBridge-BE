package com.skillbridge.controller.api.admin;

import com.skillbridge.dto.admin.response.SkillListResponse;
import com.skillbridge.dto.admin.response.SkillResponseDTO;
import com.skillbridge.service.admin.AdminSkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin Master Data Skill Controller
 * Handles skill management endpoints for Admin Master Data
 * Note: context-path is /api, so full path will be /api/admin/master-data/skills
 */
@RestController
@RequestMapping("/admin/master-data/skills")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:4200"}, 
             allowCredentials = "true",
             maxAge = 3600)
public class AdminMasterDataSkillController {

    @Autowired
    private AdminSkillService adminSkillService;

    /**
     * Get all parent skills with pagination and search
     * GET /api/admin/master-data/skills
     * 
     * Query parameters:
     * - page: Page number (default: 0)
     * - size: Page size (default: 20)
     * - search: Search term (optional)
     */
    @GetMapping
    public ResponseEntity<?> getAllParentSkills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        try {
            SkillListResponse response = adminSkillService.getAllParentSkills(page, size, search);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to get skills: " + e.getMessage()));
        }
    }

    /**
     * Get sub-skills for a parent skill
     * GET /api/admin/master-data/skills/{skillId}/sub-skills
     */
    @GetMapping("/{skillId}/sub-skills")
    public ResponseEntity<?> getSubSkills(@PathVariable Integer skillId) {
        try {
            List<SkillResponseDTO> subSkills = adminSkillService.getSubSkillsByParentId(skillId);
            return ResponseEntity.ok(subSkills);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to get sub-skills: " + e.getMessage()));
        }
    }

    /**
     * Error response class
     */
    private static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}

