package com.skillbridge.controller.api.homepage;

import com.skillbridge.dto.common.HomepageStatistics;
import com.skillbridge.dto.engineer.response.EngineerProfile;
import com.skillbridge.service.common.HomepageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public")
@CrossOrigin(origins = "*")
public class HomepageController {

    @Autowired
    private HomepageService homepageService;

    /**
     * Get homepage statistics
     * GET /api/public/homepage/statistics
     */
    @GetMapping("/homepage/statistics")
    public ResponseEntity<HomepageStatistics> getHomepageStatistics() {
        HomepageStatistics stats = homepageService.getHomepageStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get all homepage engineers (grouped by categories)
     * GET /api/public/homepage/engineers
     */
    @GetMapping("/homepage/engineers")
    public ResponseEntity<List<EngineerProfile>> getHomepageEngineers() {
        List<EngineerProfile> engineers = homepageService.getAllCategoryEngineers();
        return ResponseEntity.ok(engineers);
    }

    /**
     * Get engineers by specific category
     * GET /api/public/homepage/engineers/{category}
     */
    @GetMapping("/homepage/engineers/{category}")
    public ResponseEntity<List<EngineerProfile>> getEngineersByCategory(@PathVariable String category) {
        List<EngineerProfile> engineers = homepageService.getEngineersByCategory(category);
        return ResponseEntity.ok(engineers);
    }
}

