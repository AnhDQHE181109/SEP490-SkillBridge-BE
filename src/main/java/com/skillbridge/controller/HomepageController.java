package com.skillbridge.controller;

import com.skillbridge.dto.EngineerProfileDTO;
import com.skillbridge.dto.HomepageStatisticsDTO;
import com.skillbridge.entity.Engineer;
import com.skillbridge.service.HomepageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public")
@CrossOrigin(origins = "*")
public class HomepageController {

    @Autowired
    private HomepageService homepageService;

    @GetMapping("/homepage/statistics")
    public ResponseEntity<HomepageStatisticsDTO> getHomepageStatistics() {
        HomepageStatisticsDTO homepageStatisticsDTO = homepageService.getHomepageStatistics();
        return new ResponseEntity<>(homepageStatisticsDTO, HttpStatus.OK);
    }

    @GetMapping("/homepage/engineers")
    public ResponseEntity<List<EngineerProfileDTO>> getEngineersList() {
        List<EngineerProfileDTO> engineersList = homepageService.getAllCategoryEngineers();
        return new ResponseEntity<>(engineersList, HttpStatus.OK);
    }

    /**
     * Get engineers by specific category
     * GET /api/public/homepage/engineers/{category}
     */
    @GetMapping("/homepage/engineers/{category}")
    public ResponseEntity<List<EngineerProfileDTO>> getEngineersByCategory(@PathVariable String category) {
        List<EngineerProfileDTO> engineersListByCategory = homepageService.getEngineersByCategory(category);
        return new ResponseEntity<>(engineersListByCategory, HttpStatus.OK);
    }

}
