package com.skillbridge.service.admin;

import com.skillbridge.dto.admin.request.*;
import com.skillbridge.dto.admin.response.PageInfo;
import com.skillbridge.dto.admin.response.SkillListResponse;
import com.skillbridge.dto.admin.response.SkillResponseDTO;
import com.skillbridge.entity.engineer.Skill;
import com.skillbridge.repository.engineer.EngineerSkillRepository;
import com.skillbridge.repository.engineer.SkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Skill Service
 * Handles all skill management operations for Admin Master Data
 */
@Service
@Transactional
public class AdminSkillService {

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private EngineerSkillRepository engineerSkillRepository;

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
     * Update a skill
     */
    public SkillResponseDTO updateSkill(Integer skillId, UpdateSkillRequest request) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found"));

        // Check if skill is a parent skill
        if (skill.getParentSkillId() != null) {
            throw new RuntimeException("Cannot update a sub-skill using this endpoint. Use update sub-skill endpoint.");
        }

        // Check if new name conflicts with existing skill (excluding current skill)
        skillRepository.findByNameAndParentSkillIdIsNull(request.getName())
                .ifPresent(s -> {
                    if (!s.getId().equals(skillId)) {
                        throw new RuntimeException("Skill name already exists: " + request.getName());
                    }
                });

        // Update skill
        skill.setName(request.getName());
        skill.setDescription(request.getDescription());

        skill = skillRepository.save(skill);

        // Update sub-skills if provided
        if (request.getSubSkills() != null) {
            // Get existing sub-skills
            List<Skill> existingSubSkills = skillRepository.findByParentSkillId(skillId);
            List<Integer> existingSubSkillIds = existingSubSkills.stream()
                    .map(Skill::getId)
                    .collect(Collectors.toList());

            // Process updated sub-skills
            List<Integer> updatedSubSkillIds = new ArrayList<>();
            for (UpdateSkillRequest.SubSkillUpdateRequest subSkillReq : request.getSubSkills()) {
                if (subSkillReq.getId() != null) {
                    // Update existing sub-skill
                    Skill existingSubSkill = skillRepository.findById(subSkillReq.getId())
                            .orElseThrow(() -> new RuntimeException("Sub-skill not found: " + subSkillReq.getId()));

                    // Check if sub-skill belongs to this parent
                    if (!existingSubSkill.getParentSkillId().equals(skillId)) {
                        throw new RuntimeException("Sub-skill does not belong to this parent skill");
                    }

                    // Check if new name conflicts (excluding current sub-skill)
                    skillRepository.findByNameAndParentSkillId(subSkillReq.getName(), skillId)
                            .ifPresent(s -> {
                                if (!s.getId().equals(subSkillReq.getId())) {
                                    throw new RuntimeException("Sub-skill name already exists: " + subSkillReq.getName());
                                }
                            });

                    existingSubSkill.setName(subSkillReq.getName());
                    skillRepository.save(existingSubSkill);
                    updatedSubSkillIds.add(subSkillReq.getId());
                } else {
                    // Create new sub-skill
                    // Check if sub-skill name already exists for this parent
                    skillRepository.findByNameAndParentSkillId(subSkillReq.getName(), skillId)
                            .ifPresent(s -> {
                                throw new RuntimeException("Sub-skill name already exists: " + subSkillReq.getName());
                            });

                    Skill newSubSkill = new Skill();
                    newSubSkill.setName(subSkillReq.getName());
                    newSubSkill.setParentSkillId(skillId);
                    Skill savedSubSkill = skillRepository.save(newSubSkill);
                    updatedSubSkillIds.add(savedSubSkill.getId());
                }
            }

            // Delete sub-skills that are not in the updated list
            List<Integer> toDelete = existingSubSkillIds.stream()
                    .filter(id -> !updatedSubSkillIds.contains(id))
                    .collect(Collectors.toList());

            for (Integer subSkillId : toDelete) {
                // Check if sub-skill is in use before deleting
                if (isSubSkillInUse(subSkillId)) {
                    throw new RuntimeException("Cannot delete sub-skill. It is currently in use by engineers.");
                }
                skillRepository.deleteById(subSkillId);
            }
        }

        return convertToDTO(skill);
    }

    /**
     * Update a sub-skill
     */
    public SkillResponseDTO updateSubSkill(Integer subSkillId, UpdateSubSkillRequest request) {
        Skill subSkill = skillRepository.findById(subSkillId)
                .orElseThrow(() -> new RuntimeException("Sub-skill not found"));

        // Check if skill is a sub-skill
        if (subSkill.getParentSkillId() == null) {
            throw new RuntimeException("Cannot update a parent skill using this endpoint. Use update skill endpoint.");
        }

        // Check if new name conflicts with existing sub-skill (excluding current sub-skill)
        skillRepository.findByNameAndParentSkillId(request.getName(), subSkill.getParentSkillId())
                .ifPresent(s -> {
                    if (!s.getId().equals(subSkillId)) {
                        throw new RuntimeException("Sub-skill name already exists for this parent: " + request.getName());
                    }
                });

        // Update sub-skill
        subSkill.setName(request.getName());

        subSkill = skillRepository.save(subSkill);

        return convertToDTO(subSkill);
    }

    /**
     * Check if a skill is in use by engineers
     */
    public boolean isSkillInUse(Integer skillId) {
        List<com.skillbridge.entity.engineer.EngineerSkill> engineerSkills = engineerSkillRepository.findBySkillId(skillId);
        return !engineerSkills.isEmpty();
    }

    /**
     * Check if a sub-skill is in use by engineers
     */
    public boolean isSubSkillInUse(Integer subSkillId) {
        List<com.skillbridge.entity.engineer.EngineerSkill> engineerSkills = engineerSkillRepository.findBySkillId(subSkillId);
        return !engineerSkills.isEmpty();
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
