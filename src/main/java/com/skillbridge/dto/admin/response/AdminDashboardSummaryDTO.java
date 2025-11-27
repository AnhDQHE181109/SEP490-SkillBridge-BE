package com.skillbridge.dto.admin.response;

/**
 * Admin Dashboard Summary DTO
 * Contains summary statistics for the admin dashboard
 */
public class AdminDashboardSummaryDTO {
    private EngineerSummary engineers;
    private SystemUserSummary systemUsers;
    private SkillSummary skills;
    private ProjectTypeSummary projectTypes;

    public AdminDashboardSummaryDTO() {
    }

    public EngineerSummary getEngineers() {
        return engineers;
    }

    public void setEngineers(EngineerSummary engineers) {
        this.engineers = engineers;
    }

    public SystemUserSummary getSystemUsers() {
        return systemUsers;
    }

    public void setSystemUsers(SystemUserSummary systemUsers) {
        this.systemUsers = systemUsers;
    }

    public SkillSummary getSkills() {
        return skills;
    }

    public void setSkills(SkillSummary skills) {
        this.skills = skills;
    }

    public ProjectTypeSummary getProjectTypes() {
        return projectTypes;
    }

    public void setProjectTypes(ProjectTypeSummary projectTypes) {
        this.projectTypes = projectTypes;
    }

    /**
     * Engineer Summary
     */
    public static class EngineerSummary {
        private Integer active;
        private Integer inactive;

        public EngineerSummary() {
        }

        public EngineerSummary(Integer active, Integer inactive) {
            this.active = active;
            this.inactive = inactive;
        }

        public Integer getActive() {
            return active;
        }

        public void setActive(Integer active) {
            this.active = active;
        }

        public Integer getInactive() {
            return inactive;
        }

        public void setInactive(Integer inactive) {
            this.inactive = inactive;
        }
    }

    /**
     * System User Summary
     */
    public static class SystemUserSummary {
        private Integer active;
        private Integer inactive;

        public SystemUserSummary() {
        }

        public SystemUserSummary(Integer active, Integer inactive) {
            this.active = active;
            this.inactive = inactive;
        }

        public Integer getActive() {
            return active;
        }

        public void setActive(Integer active) {
            this.active = active;
        }

        public Integer getInactive() {
            return inactive;
        }

        public void setInactive(Integer inactive) {
            this.inactive = inactive;
        }
    }

    /**
     * Skill Summary
     */
    public static class SkillSummary {
        private Integer total;

        public SkillSummary() {
        }

        public SkillSummary(Integer total) {
            this.total = total;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }
    }

    /**
     * Project Type Summary
     */
    public static class ProjectTypeSummary {
        private Integer total;

        public ProjectTypeSummary() {
        }

        public ProjectTypeSummary(Integer total) {
            this.total = total;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }
    }
}

