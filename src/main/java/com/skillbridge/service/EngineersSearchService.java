package com.skillbridge.service;

import com.skillbridge.repository.EngineersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EngineersSearchService {

    @Autowired
    private EngineersRepository engineersRepository;

    /**
     * This method returns available engineers' skills
     * @return List<String> List of available skills
     */
    public List<String> getAvailableSkills() {
        return engineersRepository.findDistinctPrimarySkills();
    }

    /**
     * This method returns available engineers' locations
     * @return List<String> List of available locations
     */
    public List<String> getAvailableLocations() {
        return engineersRepository.findDistinctLocations();
    }

    /**
     * This method returns available engineers' seniority levels
     * @return List<String> List of available seniority levels
     */
    public List<String> getAvailableSeniorityLevels() {
        return engineersRepository.findDistinctSeniorityLevels();
    }

}
