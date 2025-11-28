package com.skillbridge.service.common;

import com.skillbridge.dto.common.HomepageStatistics;
import com.skillbridge.dto.engineer.response.EngineerProfile;
import com.skillbridge.entity.engineer.Engineer;
import com.skillbridge.repository.engineer.EngineerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HomepageService {

    @Autowired
    private EngineerRepository engineerRepository;

    @Autowired(required = false)
    private S3Service s3Service;

    /**
     * Get homepage statistics (total engineers and customers)
     */
    public HomepageStatistics getHomepageStatistics() {
        Long totalEngineers = engineerRepository.countByStatus("AVAILABLE");
        // TODO: Implement customer count when contacts table is created
        Long totalCustomers = 30L; // Default value for now
        
        return new HomepageStatistics(totalEngineers, totalCustomers);
    }

    /**
     * Get featured engineers for homepage
     */
    public List<EngineerProfile> getFeaturedEngineers() {
        List<Engineer> engineers = engineerRepository.findFeaturedEngineers();
        return engineers.stream()
                .limit(9) // Get 9 engineers total (3 per category)
                .map(this::convertToProfile)
                .collect(Collectors.toList());
    }

    /**
     * Get engineers by category
     */
    public List<EngineerProfile> getEngineersByCategory(String category) {
        List<Engineer> engineers;
        
        switch (category.toLowerCase()) {
            case "web":
                engineers = engineerRepository.findWebDevelopers();
                break;
            case "game":
                engineers = engineerRepository.findGameDevelopers();
                break;
            case "ai-ml":
            case "aiml":
                engineers = engineerRepository.findAiMlDevelopers();
                break;
            default:
                engineers = engineerRepository.findByCategory(category);
                break;
        }
        
        return engineers.stream()
                .limit(3) // Get 3 engineers per category
                .map(this::convertToProfile)
                .collect(Collectors.toList());
    }

    /**
     * Get engineers grouped by all categories
     */
    public List<EngineerProfile> getAllCategoryEngineers() {
        List<EngineerProfile> allEngineers = new java.util.ArrayList<>();
        
        // Get 3 web developers
        List<EngineerProfile> webDevs = getEngineersByCategory("web");
        allEngineers.addAll(webDevs);
        
        // Get 3 game developers
        List<EngineerProfile> gameDevs = getEngineersByCategory("game");
        allEngineers.addAll(gameDevs);
        
        // Get 3 AI/ML developers
        List<EngineerProfile> aiMlDevs = getEngineersByCategory("ai-ml");
        allEngineers.addAll(aiMlDevs);
        
        return allEngineers;
    }

    /**
     * Convert Engineer entity to EngineerProfile DTO
     */
    private EngineerProfile convertToProfile(Engineer engineer) {
        EngineerProfile profile = new EngineerProfile();
        profile.setId(engineer.getId());
        profile.setFullName(engineer.getFullName());
        profile.setCategory(mapCategory(engineer.getPrimarySkill()));
        profile.setSeniority(engineer.getSeniority());
        profile.setSalaryExpectation(engineer.getSalaryExpectation());
        profile.setYearsExperience(engineer.getYearsExperience());
        profile.setLocation(engineer.getLocation());
        
        // Generate presigned URL if profileImageUrl is S3 key (not a full URL)
        String profileImageUrl = engineer.getProfileImageUrl();
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            // If it's not a URL (doesn't start with http:// or https://), treat it as S3 key
            if (!profileImageUrl.startsWith("http://") && !profileImageUrl.startsWith("https://")) {
                // Generate presigned URL with 24 hours expiration
                if (s3Service != null) {
                    try {
                        profileImageUrl = s3Service.getPresignedUrl(profileImageUrl, 24 * 60); // 24 hours
                    } catch (Exception e) {
                        // If S3 service is not available or key is invalid, use original value
                        System.err.println("Failed to generate presigned URL for S3 key: " + profileImageUrl);
                    }
                }
            }
        }
        profile.setProfileImageUrl(profileImageUrl);
        
        profile.setPrimarySkill(engineer.getPrimarySkill());
        profile.setStatus(engineer.getStatus());
        profile.setSummary(engineer.getSummary());
        profile.setLanguageSummary(engineer.getLanguageSummary());
        return profile;
    }

    /**
     * Map primary skill to category
     */
    private String mapCategory(String primarySkill) {
        if (primarySkill == null) return "web";
        
        String skill = primarySkill.toLowerCase();
        
        if (skill.contains("web") || skill.contains("frontend") || skill.contains("backend") ||
            skill.contains("react") || skill.contains("angular") || skill.contains("vue")) {
            return "web";
        }
        
        if (skill.contains("game") || skill.contains("unity") || skill.contains("unreal") ||
            skill.contains("godot")) {
            return "game";
        }
        
        if (skill.contains("ai") || skill.contains("ml") || skill.contains("machine learning") ||
            skill.contains("artificial intelligence") || skill.contains("deep learning") ||
            skill.contains("data science")) {
            return "ai-ml";
        }
        
        return "web"; // Default to web
    }
}

