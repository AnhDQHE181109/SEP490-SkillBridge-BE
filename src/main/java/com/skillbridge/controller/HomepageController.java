package com.skillbridge.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
@CrossOrigin(origins = "*")
public class HomepageController {

    @GetMapping("/homepage/statistics")
    public String homepage(){
        // Temporary placeholder response. Replace with real statistics payload when available.
        return "{}";
    }

}
