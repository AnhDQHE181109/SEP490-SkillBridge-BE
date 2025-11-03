package com.skillbridge.service;

import com.skillbridge.dto.EngineerProfileDTO;
import com.skillbridge.dto.HomepageStatisticsDTO;
import com.skillbridge.entity.Engineer;
import com.skillbridge.repository.EngineersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HomepageService {

    @Autowired
    private EngineersRepository engineersRepository;

    public HomepageStatisticsDTO getHomepageStatistics() {
        Long totalEngineers = engineersRepository.countEngineersByStatus("AVAILABLE");

        return new HomepageStatisticsDTO(totalEngineers, 0L);
    }

    /**
     * Get engineers by category
     */
    public List<EngineerProfileDTO> getEngineersByCategory(String category) {
        List<Engineer> engineers;

        switch (category.toLowerCase()) {
            case "web":
                engineers = engineersRepository.findWebDevelopers();
                break;
            case "game":
                engineers = engineersRepository.findGameDevelopers();
                break;
            case "ai-ml":
            case "aiml":
                engineers = engineersRepository.findAiMlDevelopers();
                break;
            default:
                engineers = engineersRepository.findByCategory(category);
                break;
        }

        return engineers.stream()
                .limit(3) // Get 3 engineers per category
                .map(this::convertToProfile)
                .collect(Collectors.toList());
    }

    /**
     * Convert Engineer entity to EngineerProfile DTO
     */
    private EngineerProfileDTO convertToProfile(Engineer engineer) {
        EngineerProfileDTO profile = new EngineerProfileDTO();
        profile.setId(engineer.getID());
        profile.setFullName(engineer.getFullName());
        profile.setCategory(mapCategory(engineer.getPrimarySkill()));
        profile.setSeniority(engineer.getSeniority());
        profile.setSalaryExpectation(engineer.getSalaryExpectation());
        profile.setYearsExperience(engineer.getYearsOfExperience());
        profile.setLocation(engineer.getLocation());
        profile.setProfileImageUrl(engineer.getProfileImageURL());
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

    /**
     * Get engineers grouped by all categories
     */
    public List<EngineerProfileDTO> getAllCategoryEngineers() {
        List<EngineerProfileDTO> allEngineers = new ArrayList<>();

        // Get 3 web developers
        List<EngineerProfileDTO> webDevs = getEngineersByCategory("web");
        allEngineers.addAll(webDevs);

        // Get 3 game developers
        List<EngineerProfileDTO> gameDevs = getEngineersByCategory("game");
        allEngineers.addAll(gameDevs);

        // Get 3 AI/ML developers
        List<EngineerProfileDTO> aiMlDevs = getEngineersByCategory("ai-ml");
        allEngineers.addAll(aiMlDevs);

        return allEngineers;
    }
}
