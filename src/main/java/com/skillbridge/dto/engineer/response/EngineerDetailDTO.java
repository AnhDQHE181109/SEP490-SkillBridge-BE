package com.skillbridge.dto.engineer.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Engineer Detail DTO
 * Complete engineer profile information for detail view
 */
public class EngineerDetailDTO {
    private Integer id;
    private String fullName;
    private String location;
    private String profileImageUrl;
    private BigDecimal salaryExpectation;
    private Integer yearsExperience;
    private String seniority;
    private String status;
    private String primarySkill;
    private String languageSummary;
    private String summary;
    private String introduction;
    private List<SkillDTO> skills;
    private List<CertificateDTO> certificates;

    // Constructors
    public EngineerDetailDTO() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public BigDecimal getSalaryExpectation() {
        return salaryExpectation;
    }

    public void setSalaryExpectation(BigDecimal salaryExpectation) {
        this.salaryExpectation = salaryExpectation;
    }

    public Integer getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(Integer yearsExperience) {
        this.yearsExperience = yearsExperience;
    }

    public String getSeniority() {
        return seniority;
    }

    public void setSeniority(String seniority) {
        this.seniority = seniority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrimarySkill() {
        return primarySkill;
    }

    public void setPrimarySkill(String primarySkill) {
        this.primarySkill = primarySkill;
    }

    public String getLanguageSummary() {
        return languageSummary;
    }

    public void setLanguageSummary(String languageSummary) {
        this.languageSummary = languageSummary;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public List<SkillDTO> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillDTO> skills) {
        this.skills = skills;
    }

    public List<CertificateDTO> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<CertificateDTO> certificates) {
        this.certificates = certificates;
    }

    @Override
    public String toString() {
        return "EngineerDetailDTO{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", location='" + location + '\'' +
                ", seniority='" + seniority + '\'' +
                ", status='" + status + '\'' +
                ", primarySkill='" + primarySkill + '\'' +
                ", skills=" + (skills != null ? skills.size() : 0) +
                ", certificates=" + (certificates != null ? certificates.size() : 0) +
                '}';
    }
}

