package com.skillbridge.controller;

import com.skillbridge.dto.EngineerDetailsDTO;
import com.skillbridge.dto.EngineerDetailsDTO;
import com.skillbridge.service.EngineerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Engineer Detail Controller
 * REST API endpoints for viewing engineer details
 */
@RestController
@RequestMapping("/public/engineers")
@CrossOrigin(origins = "*")
public class EngineerDetailsController {

    @Autowired
    private EngineerService engineerService;

    /**
     * Get engineer detail by ID
     * GET /api/public/engineers/{id}
     *
     * @param id Engineer ID
     * @return Engineer detail information with skills and certificates
     */
    @GetMapping("/{id}")
    public ResponseEntity<EngineerDetailsDTO> getEngineerById(@PathVariable Integer id) {
        EngineerDetailsDTO engineer = engineerService.getEngineerDetailById(id);

        if (engineer == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(engineer);
    }
}

