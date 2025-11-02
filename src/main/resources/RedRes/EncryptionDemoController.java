package com.skillbridge.controller;

import com.skillbridge.service.PasswordEncryptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/demo")
@CrossOrigin
public class EncryptionDemoController {

    private final PasswordEncryptionService passwordEncryptionService;

    public EncryptionDemoController(PasswordEncryptionService passwordEncryptionService) {
        this.passwordEncryptionService = passwordEncryptionService;
    }

    /**
     * Demo endpoint to show password hashing
     * 
     * GET /api/demo/hash?password=yourpassword
     */
    @GetMapping("/hash")
    public ResponseEntity<Map<String, Object>> hashPassword(@RequestParam String password) {
        String hashedPassword = passwordEncryptionService.hashPassword(password);
        
        Map<String, Object> response = new HashMap<>();
        response.put("originalPassword", password);
        response.put("hashedPassword", hashedPassword);
        response.put("algorithm", "BCrypt");
        response.put("strength", "12 rounds (2^12 = 4096 iterations)");
        response.put("note", "Each hash is unique due to random salt");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Demo endpoint to verify password against hash
     * 
     * POST /api/demo/verify
     * Body: {"password": "yourpassword", "hash": "hashedvalue"}
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPassword(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        String hash = request.get("hash");
        
        boolean matches = passwordEncryptionService.verifyPassword(password, hash);
        
        Map<String, Object> response = new HashMap<>();
        response.put("password", password);
        response.put("hash", hash);
        response.put("matches", matches);
        response.put("method", "BCrypt matches() comparison");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Demo endpoint to show multiple hashes of the same password
     * 
     * GET /api/demo/multiple-hashes?password=yourpassword
     */
    @GetMapping("/multiple-hashes")
    public ResponseEntity<Map<String, Object>> multipleHashes(@RequestParam String password) {
        Map<String, Object> response = new HashMap<>();
        response.put("originalPassword", password);
        response.put("hash1", passwordEncryptionService.hashPassword(password));
        response.put("hash2", passwordEncryptionService.hashPassword(password));
        response.put("hash3", passwordEncryptionService.hashPassword(password));
        response.put("explanation", "Notice how each hash is different despite same input - this is due to random salt");
        
        return ResponseEntity.ok(response);
    }
}