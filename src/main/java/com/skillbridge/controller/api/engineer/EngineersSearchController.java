package com.skillbridge.controller.api.engineer;

import com.skillbridge.dto.engineer.response.EngineersSearchResponse;
import com.skillbridge.dto.engineer.request.SearchCriteria;
import com.skillbridge.service.engineer.EngineersSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/public/engineers")
@CrossOrigin(origins = "*")
public class EngineersSearchController {

    @Autowired
    private EngineersSearchService engineersSearchService;

    @GetMapping("/search")
    public ResponseEntity<EngineersSearchResponse> searchEngineers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<String> skills,
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
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setQuery(query);
        searchCriteria.setSkills(skills);
        searchCriteria.setExperienceMin(experienceMin);
        searchCriteria.setExperienceMax(experienceMax);
        searchCriteria.setSeniority(seniority);
        searchCriteria.setLocation(location);
        searchCriteria.setSalaryMin(salaryMin);
        searchCriteria.setSalaryMax(salaryMax);
        searchCriteria.setAvailability(availability);
        searchCriteria.setPage(page);
        searchCriteria.setSize(size);
        searchCriteria.setSortBy(sortBy);

        EngineersSearchResponse engineersSearchResponse = engineersSearchService.searchEngineers(searchCriteria);

        return new ResponseEntity<>(engineersSearchResponse, HttpStatus.OK);
    }

    /**
     * This method responds to the frontend's request for available skills
     * @return ResponseEntity<List<String>> ResponseEntity containing available skills
     */
    @GetMapping("/filters/skills")
    public ResponseEntity<List<String>> getAvailableSkills() {
        List<String> skills = engineersSearchService.getAvailableSkills();

        return new ResponseEntity<>(skills, HttpStatus.OK);
    }

    /**
     * This method responds to the frontend's request for available locations
     * @return ResponseEntity<List<String>> ResponseEntity containing available locations
     */
    @GetMapping("/filters/locations")
    public ResponseEntity<List<String>> getAvailableLocations() {
        List<String> locations = engineersSearchService.getAvailableLocations();

        return new ResponseEntity<>(locations, HttpStatus.OK);
    }

    /**
     * This method responds to the frontend's request for available seniority levels
     * @return ResponseEntity<List<String>> ResponseEntity containing available seniority levels
     */
    @GetMapping("/filters/seniorities")
    public ResponseEntity<List<String>> getAvailableSeniorities() {
        List<String> seniorityLevels = engineersSearchService.getAvailableSeniorityLevels();

        return new ResponseEntity<>(seniorityLevels, HttpStatus.OK);
    }

}
