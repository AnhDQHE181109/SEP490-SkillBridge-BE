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

        return engineers;
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
