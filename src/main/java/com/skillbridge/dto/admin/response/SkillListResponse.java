package com.skillbridge.dto.admin.response;

import java.util.List;

/**
 * Skill List Response DTO
 * Response DTO for paginated skill list
 */
public class SkillListResponse {
    private List<SkillResponseDTO> content;
    private PageInfo page;

    // Constructors
    public SkillListResponse() {
    }

    public SkillListResponse(List<SkillResponseDTO> content, PageInfo page) {
        this.content = content;
        this.page = page;
    }

    // Getters and Setters
    public List<SkillResponseDTO> getContent() {
        return content;
    }

    public void setContent(List<SkillResponseDTO> content) {
        this.content = content;
    }

    public PageInfo getPage() {
        return page;
    }

    public void setPage(PageInfo page) {
        this.page = page;
    }

}

