package com.skillbridge.controller;

import com.skillbridge.dto.HomepageStatisticsDTO;
import com.skillbridge.entity.Engineer;
import com.skillbridge.service.HomepageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public ResponseEntity<List<Engineer>> getEngineersList() {

    }

}
