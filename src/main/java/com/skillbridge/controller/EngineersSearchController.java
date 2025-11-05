package com.skillbridge.controller;

import com.skillbridge.service.EngineersSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public/engineers")
@CrossOrigin(origins = "*")
public class EngineersSearchController {

    @Autowired
    private EngineersSearchService engineersSearchService;

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
