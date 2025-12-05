package com.skillbridge.service.admin;

import com.skillbridge.dto.admin.request.CreateSkillRequest;
import com.skillbridge.dto.admin.request.CreateSubSkillRequest;
import com.skillbridge.dto.admin.response.SkillListResponse;
import com.skillbridge.dto.admin.response.SkillResponseDTO;
import com.skillbridge.entity.engineer.Skill;
import com.skillbridge.repository.engineer.EngineerSkillRepository;
import com.skillbridge.repository.engineer.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AdminSkillService}
 */
@ExtendWith(MockitoExtension.class)
class AdminSkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private EngineerSkillRepository engineerSkillRepository;

    @InjectMocks
    private AdminSkillService adminSkillService;

    private Skill parentSkill;

    @BeforeEach
    void setUp() {
        parentSkill = new Skill();
        parentSkill.setId(1);
        parentSkill.setName("Programming");
        parentSkill.setDescription("Programming related");
        parentSkill.setParentSkillId(null);
    }

    @Test
    @DisplayName("getAllParentSkills - without search returns parent skills")
    void testGetAllParentSkills_NoSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Skill> page = new PageImpl<>(List.of(parentSkill), pageable, 1);
        when(skillRepository.findByParentSkillIdIsNull(pageable)).thenReturn(page);

        SkillListResponse response = adminSkillService.getAllParentSkills(0, 10, null);

        assertEquals(1, response.getContent().size());
        assertEquals("Programming", response.getContent().get(0).getName());
        verify(skillRepository).findByParentSkillIdIsNull(pageable);
    }

    @Test
    @DisplayName("getAllParentSkills - with search delegates to repository search")
    void testGetAllParentSkills_WithSearch() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Skill> page = new PageImpl<>(List.of(parentSkill), pageable, 1);
        when(skillRepository.searchParentSkills("prog", pageable)).thenReturn(page);

        SkillListResponse response = adminSkillService.getAllParentSkills(0, 5, " prog ");

        assertEquals(1, response.getContent().size());
        verify(skillRepository).searchParentSkills("prog", pageable);
    }

    @Test
    @DisplayName("getSubSkillsByParentId - throws when parent not found")
    void testGetSubSkillsByParentId_NotFound() {
        when(skillRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> adminSkillService.getSubSkillsByParentId(1));
    }

    @Test
    @DisplayName("getSubSkillsByParentId - throws when skill is not parent")
    void testGetSubSkillsByParentId_NotParent() {
        Skill sub = new Skill();
        sub.setId(2);
        sub.setParentSkillId(1);
        when(skillRepository.findById(2)).thenReturn(Optional.of(sub));

        assertThrows(RuntimeException.class, () -> adminSkillService.getSubSkillsByParentId(2));
    }

    @Test
    @DisplayName("createSkill - throws when name already exists")
    void testCreateSkill_NameExists() {
        CreateSkillRequest request = new CreateSkillRequest();
        request.setName("Programming");

        when(skillRepository.findByNameAndParentSkillIdIsNull("Programming")).thenReturn(Optional.of(parentSkill));

        assertThrows(RuntimeException.class, () -> adminSkillService.createSkill(request));
        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    @DisplayName("createSkill - saves parent and sub-skills")
    void testCreateSkill_Success() {
        CreateSkillRequest request = new CreateSkillRequest();
        request.setName("Programming");
        request.setDescription("All programming");
        CreateSkillRequest.SubSkillRequest subSkillRequest = new CreateSkillRequest.SubSkillRequest();
        subSkillRequest.setName("Java");
        request.setSubSkills(List.of(subSkillRequest));

        Skill savedParent = new Skill();
        savedParent.setId(1);
        savedParent.setName("Programming");

        when(skillRepository.findByNameAndParentSkillIdIsNull("Programming")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenReturn(savedParent);

        SkillResponseDTO response = adminSkillService.createSkill(request);

        assertEquals("Programming", response.getName());
        verify(skillRepository, times(2)).save(any(Skill.class));
    }

    @Test
    @DisplayName("createSubSkill - throws when parent is not found or not parent")
    void testCreateSubSkill_ParentValidation() {
        when(skillRepository.findById(1)).thenReturn(Optional.empty());
        CreateSubSkillRequest request = new CreateSubSkillRequest();
        request.setName("Java");

        assertThrows(RuntimeException.class, () -> adminSkillService.createSubSkill(1, request));

        Skill notParent = new Skill();
        notParent.setId(2);
        notParent.setParentSkillId(1);
        when(skillRepository.findById(1)).thenReturn(Optional.of(notParent));

        assertThrows(RuntimeException.class, () -> adminSkillService.createSubSkill(1, request));
    }

    @Test
    @DisplayName("deleteSkill - throws when skill is in use")
    void testDeleteSkill_InUse() {
        when(skillRepository.findById(1)).thenReturn(Optional.of(parentSkill));
        when(engineerSkillRepository.findBySkillId(1)).thenReturn(List.of(new com.skillbridge.entity.engineer.EngineerSkill()));

        assertThrows(RuntimeException.class, () -> adminSkillService.deleteSkill(1));
    }
}

