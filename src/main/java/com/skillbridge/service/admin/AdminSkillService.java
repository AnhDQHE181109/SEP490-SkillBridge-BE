package com.skillbridge.service.admin;

import com.skillbridge.dto.admin.request.CreateSkillRequest;
import com.skillbridge.dto.admin.request.CreateSubSkillRequest;
import com.skillbridge.dto.admin.request.SubSkillRequest;
import com.skillbridge.dto.admin.response.PageInfo;
import com.skillbridge.dto.admin.response.SkillListResponse;
import com.skillbridge.dto.admin.response.SkillResponseDTO;
import com.skillbridge.entity.engineer.Skill;
import com.skillbridge.repository.engineer.SkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Skill Service
 * Handles all skill management operations for Admin Master Data
 */
@Service

public class AdminSkillService {

    @Autowired
    private SkillRepository skillRepository;

    /**
     * Get all parent skills with pagination and search
     */
    public SkillListResponse getAllParentSkills(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Skill> skillPage;

        if (search != null && !search.trim().isEmpty()) {
            skillPage = skillRepository.searchParentSkills(search.trim(), pageable);
        } else {
            skillPage = skillRepository.findByParentSkillIdIsNull(pageable);
        }

        List<SkillResponseDTO> content = skillPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        PageInfo pageInfo = new PageInfo(
                skillPage.getNumber(),
                skillPage.getSize(),
                skillPage.getTotalElements(),
                skillPage.getTotalPages()
        );

        SkillListResponse response = new SkillListResponse();
        response.setContent(content);
        response.setPage(pageInfo);

        return response;
    }

    /**
     * Get all sub-skills for a parent skill
     */
    public List<SkillResponseDTO> getSubSkillsByParentId(Integer parentSkillId) {
        Skill parentSkill = skillRepository.findById(parentSkillId)
                .orElseThrow(() -> new RuntimeException("Parent skill not found"));

        if (parentSkill.getParentSkillId() != null) {
            throw new RuntimeException("Specified skill is not a parent skill");
        }

        List<Skill> subSkills = skillRepository.findByParentSkillId(parentSkillId);
        return subSkills.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new skill with optional sub-skills
     */
    public SkillResponseDTO createSkill(CreateSkillRequest request) {
        // Check if skill name already exists (parent skills must be unique)
        skillRepository.findByNameAndParentSkillIdIsNull(request.getName())
                .ifPresent(s -> {
                    throw new RuntimeException("Skill name already exists: " + request.getName());
                });

        // Create parent skill
        Skill parentSkill = new Skill();
        parentSkill.setName(request.getName());
        parentSkill.setDescription(request.getDescription());
        parentSkill.setParentSkillId(null);

        parentSkill = skillRepository.save(parentSkill);

        // Create sub-skills if provided
        if (request.getSubSkills() != null && !request.getSubSkills().isEmpty()) {
            for (SubSkillRequest subSkillReq : request.getSubSkills()) {
                // Check if sub-skill name already exists for this parent
                skillRepository.findByNameAndParentSkillId(subSkillReq.getName(), parentSkill.getId())
                        .ifPresent(s -> {
                            throw new RuntimeException("Sub-skill name already exists for this parent: " + subSkillReq.getName());
                        });

                Skill subSkill = new Skill();
                subSkill.setName(subSkillReq.getName());
                subSkill.setParentSkillId(parentSkill.getId());
                skillRepository.save(subSkill);
            }
        }

        return convertToDTO(parentSkill);
    }

    /**
     * Create a new sub-skill
     */
    public SkillResponseDTO createSubSkill(Integer parentSkillId, CreateSubSkillRequest request) {
        // Validate parent skill exists and is a parent skill
        Skill parentSkill = skillRepository.findById(parentSkillId)
                .orElseThrow(() -> new RuntimeException("Parent skill not found"));

        if (parentSkill.getParentSkillId() != null) {
            throw new RuntimeException("Specified skill is not a parent skill");
        }

        // Check if sub-skill name already exists for this parent
        skillRepository.findByNameAndParentSkillId(request.getName(), parentSkillId)
                .ifPresent(s -> {
                    throw new RuntimeException("Sub-skill name already exists for this parent: " + request.getName());
                });

        // Create sub-skill
        Skill subSkill = new Skill();
        subSkill.setName(request.getName());
        subSkill.setParentSkillId(parentSkillId);

        subSkill = skillRepository.save(subSkill);

        return convertToDTO(subSkill);
    }

    /**
     * Convert Skill entity to SkillResponseDTO
     */
    private SkillResponseDTO convertToDTO(Skill skill) {
        SkillResponseDTO dto = new SkillResponseDTO();
        dto.setId(skill.getId());
        dto.setName(skill.getName());
        dto.setDescription(skill.getDescription());
        dto.setParentSkillId(skill.getParentSkillId());
        return dto;
    }

}
