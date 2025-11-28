package com.skillbridge.service.engineer;

import com.skillbridge.dto.engineer.request.SearchCriteria;
import com.skillbridge.dto.engineer.response.EngineerProfile;
import com.skillbridge.dto.engineer.response.EngineerSearchResponse;
import com.skillbridge.entity.engineer.Engineer;
import com.skillbridge.repository.engineer.EngineerRepository;
import com.skillbridge.service.common.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EngineerSearchService {

    @Autowired
    private EngineerRepository engineerRepository;

    @Autowired(required = false)
    private S3Service s3Service;

    /**
     * Search engineers based on criteria with pagination
     */
    public EngineerSearchResponse searchEngineers(SearchCriteria criteria) {
        // Create pageable with sorting
        Pageable pageable = createPageable(criteria);

        // Get primary skill from skills list if available
        String primarySkill = (criteria.getSkills() != null && !criteria.getSkills().isEmpty()) 
            ? criteria.getSkills().get(0) 
            : null;

        // Execute search
        Page<Engineer> engineerPage = engineerRepository.searchEngineers(
            criteria.getQuery(),
            primarySkill,
            criteria.getExperienceMin(),
            criteria.getExperienceMax(),
            criteria.getSeniority(),
            criteria.getLocation(),
            criteria.getSalaryMin(),
            criteria.getSalaryMax(),
            criteria.getAvailability(),
            pageable
        );

        // Convert to DTOs
        List<EngineerProfile> profiles = engineerPage.getContent().stream()
            .map(this::convertToProfile)
            .collect(Collectors.toList());

        // Build response
        return new EngineerSearchResponse(
            profiles,
            engineerPage.getTotalElements(),
            engineerPage.getNumber(),
            engineerPage.getTotalPages(),
            engineerPage.getSize()
        );
    }

    /**
     * Get available primary skills for filters
     */
    public List<String> getAvailableSkills() {
        return engineerRepository.findDistinctPrimarySkills();
    }

    /**
     * Get available locations for filters
     */
    public List<String> getAvailableLocations() {
        return engineerRepository.findDistinctLocations();
    }

    /**
     * Get available seniority levels for filters
     */
    public List<String> getAvailableSeniorities() {
        return engineerRepository.findDistinctSeniorities();
    }

    /**
     * Convert Engineer entity to EngineerProfile DTO (public information only)
     */
    private EngineerProfile convertToProfile(Engineer engineer) {
        EngineerProfile profile = new EngineerProfile();
        profile.setId(engineer.getId());
        profile.setFullName(engineer.getFullName());
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
        
        // Truncate summary to 200 characters for preview
        if (engineer.getSummary() != null) {
            profile.setSummary(
                engineer.getSummary().length() > 200 
                    ? engineer.getSummary().substring(0, 200) + "..." 
                    : engineer.getSummary()
            );
        }
        
        profile.setLanguageSummary(engineer.getLanguageSummary());
        
        return profile;
    }

    /**
     * Create pageable with sorting based on criteria
     */
    private Pageable createPageable(SearchCriteria criteria) {
        Sort sort;
        
        switch (criteria.getSortBy() != null ? criteria.getSortBy() : "relevance") {
            case "experience":
                sort = Sort.by(Sort.Direction.DESC, "yearsExperience");
                break;
            case "seniority":
                // Custom sorting for seniority levels
                sort = Sort.by(Sort.Direction.ASC, "seniority");
                break;
            case "salary":
                sort = Sort.by(Sort.Direction.DESC, "salaryExpectation");
                break;
            default:
                // Default: sort by experience (relevance)
                sort = Sort.by(Sort.Direction.DESC, "yearsExperience");
                break;
        }
        
        return PageRequest.of(
            criteria.getPage() != null ? criteria.getPage() : 0,
            criteria.getSize() != null ? criteria.getSize() : 20,
            sort
        );
    }
}

