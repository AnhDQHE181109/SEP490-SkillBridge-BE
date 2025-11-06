package com.skillbridge.service;

import com.skillbridge.dto.EngineerProfileDTO;
import com.skillbridge.dto.EngineersSearchResponse;
import com.skillbridge.dto.SearchCriteria;
import com.skillbridge.entity.Engineer;
import com.skillbridge.repository.EngineersRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EngineersSearchService {

    private static final Logger log = LogManager.getLogger(EngineersSearchService.class);
    @Autowired
    private EngineersRepository engineersRepository;

    public EngineersSearchResponse searchEngineers(SearchCriteria searchCriteria) {
        //Initializing a pageable with sorting criterium from the search criteria
        Pageable pageable = sortingPageable(searchCriteria);

        //Getting the first skill from the list of skills fetched from the search criteria - TODO: implement full selected list
        String primarySkill;
        if (searchCriteria.getSkills() != null && !searchCriteria.getSkills().isEmpty()) {
            primarySkill = searchCriteria.getSkills().get(0);
        } else {
            primarySkill = null;
        }

        //For debugging - Printing out the SearchCriteria object
        log.info("SearchCriteria: " +  searchCriteria);

        if (searchCriteria.getSeniority() != null && searchCriteria.getSeniority().isEmpty()) {
            searchCriteria.setSeniority(null);
        }
        if (searchCriteria.getLocation() != null && searchCriteria.getLocation().isEmpty()) {
            searchCriteria.setLocation(null);
        }

        Page<Engineer> engineersPage = engineersRepository.searchEngineers(
                searchCriteria.getQuery(),
                primarySkill,
                searchCriteria.getExperienceMin(),
                searchCriteria.getExperienceMax(),
                searchCriteria.getSeniority(),
                searchCriteria.getLocation(),
                searchCriteria.getSalaryMin(),
                searchCriteria.getSalaryMax(),
                searchCriteria.getAvailability(),
                pageable
        );

        //Printing out the engineers page's elements
        log.info("EngineersPage: " +  engineersPage.getTotalElements());

        List<EngineerProfileDTO> engineerProfiles = engineersPage.getContent().stream()
                .map(this::convertToEngineerProfileDTO)
                .collect(Collectors.toList());

        return new EngineersSearchResponse(
                engineerProfiles,
                engineersPage.getTotalElements(),
                engineersPage.getNumber(),
                engineersPage.getTotalPages(),
                engineersPage.getSize()
        );
    }

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

    public Pageable sortingPageable(SearchCriteria searchCriteria) {
        Sort sort;

        String sortBy = searchCriteria.getSortBy();
        if (sortBy == null) sortBy = "relevance";

        switch(sortBy) {
            case "seniority":
                sort = Sort.by(Sort.Direction.ASC, "seniority");
                break;

            case "salary":
                sort = Sort.by(Sort.Direction.DESC, "salaryExpectation");
                break;

            case "experience":
            default:
                sort = Sort.by(Sort.Direction.DESC, "yearsOfExperience");
                break;
        }

        if (searchCriteria.getPage() == null) searchCriteria.setPage(0);
        if (searchCriteria.getSize() == null) searchCriteria.setSize(20);

        return PageRequest.of(searchCriteria.getPage(), searchCriteria.getSize(), sort);
    }

    EngineerProfileDTO convertToEngineerProfileDTO(Engineer engineer) {
        EngineerProfileDTO engineerProfileDTO = new EngineerProfileDTO();
        engineerProfileDTO.setId(engineer.getID());
        engineerProfileDTO.setFullName(engineer.getFullName());
        engineerProfileDTO.setSeniority(engineer.getSeniority());
        engineerProfileDTO.setLocation(engineer.getLocation());
        engineerProfileDTO.setSalaryExpectation(engineer.getSalaryExpectation());
        engineerProfileDTO.setYearsExperience(engineer.getYearsOfExperience());
        engineerProfileDTO.setProfileImageUrl(engineer.getProfileImageURL());
        engineerProfileDTO.setPrimarySkill(engineer.getPrimarySkill());
        engineerProfileDTO.setStatus(engineer.getStatus());

        if (engineer.getSummary() != null) {
            if (engineer.getSummary().length() > 200) {
                engineerProfileDTO.setSummary(engineer.getSummary().substring(0, 200));
            } else {
                engineerProfileDTO.setSummary(engineer.getSummary());
            }
        }

        engineerProfileDTO.setLanguageSummary(engineer.getLanguageSummary());

        return engineerProfileDTO;
    }

}
