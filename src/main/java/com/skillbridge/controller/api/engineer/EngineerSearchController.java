package com.skillbridge.controller.api.engineer;

import com.skillbridge.dto.engineer.request.SearchCriteria;
import com.skillbridge.dto.engineer.response.EngineerSearchResponse;
import com.skillbridge.service.engineer.EngineerSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/public/engineers")
@CrossOrigin(origins = "*")
public class EngineerSearchController {

    @Autowired
    private EngineerSearchService engineerSearchService;

    /**
     * Advanced engineer search with filters
     * GET /api/public/engineers/search
     */
    @GetMapping("/search")
    public ResponseEntity<EngineerSearchResponse> searchEngineers(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) List<String> skills,
        @RequestParam(required = false) List<String> languages,
        @RequestParam(required = false) Integer experienceMin,
        @RequestParam(required = false) Integer experienceMax,
        @RequestParam(required = false) List<String> seniority,
        @RequestParam(required = false) List<String> location,
        @RequestParam(required = false) BigDecimal salaryMin,
        @RequestParam(required = false) BigDecimal salaryMax,
        @RequestParam(required = false) Boolean availability,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        @RequestParam(defaultValue = "relevance") String sortBy
    ) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery(query);
        criteria.setSkills(skills);
        criteria.setLanguages(languages);
        criteria.setExperienceMin(experienceMin);
        criteria.setExperienceMax(experienceMax);
        criteria.setSeniority(seniority);
        criteria.setLocation(location);
        criteria.setSalaryMin(salaryMin);
        criteria.setSalaryMax(salaryMax);
        criteria.setAvailability(availability);
        criteria.setPage(page);
        criteria.setSize(size);
        criteria.setSortBy(sortBy);

        EngineerSearchResponse response = engineerSearchService.searchEngineers(criteria);
        return ResponseEntity.ok(response);
    }

    /**
     * Get available skills for filter dropdown
     * GET /api/public/engineers/filters/skills
     */
    @GetMapping("/filters/skills")
    public ResponseEntity<List<String>> getAvailableSkills() {
        List<String> skills = engineerSearchService.getAvailableSkills();
        return ResponseEntity.ok(skills);
    }

    /**
     * Get available locations for filter dropdown
     * GET /api/public/engineers/filters/locations
     */
    @GetMapping("/filters/locations")
    public ResponseEntity<List<String>> getAvailableLocations() {
        List<String> locations = engineerSearchService.getAvailableLocations();
        return ResponseEntity.ok(locations);
    }

    /**
     * Get available seniority levels for filter dropdown
     * GET /api/public/engineers/filters/seniorities
     */
    @GetMapping("/filters/seniorities")
    public ResponseEntity<List<String>> getAvailableSeniorities() {
        List<String> seniorities = engineerSearchService.getAvailableSeniorities();
        return ResponseEntity.ok(seniorities);
    }
}

