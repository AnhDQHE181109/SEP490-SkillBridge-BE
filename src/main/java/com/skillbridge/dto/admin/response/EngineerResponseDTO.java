package com.skillbridge.dto.admin.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Engineer Response DTO
 * Response DTO for engineer information in Admin Engineer List
 */
public class EngineerResponseDTO {
    private Integer id;
    private String fullName;
    private Integer yearsExperience;
    private String seniority;
    private String primarySkill;
    private BigDecimal salaryExpectation;
    private String location;
    private String status;
    private String summary;
    private String introduction;
    private String languageSummary;
    private String profileImageUrl;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private Boolean interestedInJapan;
    private String projectTypeExperience;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public EngineerResponseDTO() {
    }

    public EngineerResponseDTO(Integer id, String fullName, Integer yearsExperience, String seniority,
                               String primarySkill, BigDecimal salaryExpectation, String location, String status,
                               String summary, String introduction, String profileImageUrl,
                               String email, String phone, String gender, LocalDate dateOfBirth,
                               Boolean interestedInJapan, String projectTypeExperience,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.fullName = fullName;
        this.yearsExperience = yearsExperience;
        this.seniority = seniority;
        this.primarySkill = primarySkill;
        this.salaryExpectation = salaryExpectation;
        this.location = location;
        this.status = status;
        this.summary = summary;
        this.introduction = introduction;
        this.profileImageUrl = profileImageUrl;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.interestedInJapan = interestedInJapan;
        this.projectTypeExperience = projectTypeExperience;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getPrimarySkill() {
        return primarySkill;
    }

    public void setPrimarySkill(String primarySkill) {
        this.primarySkill = primarySkill;
    }

    public BigDecimal getSalaryExpectation() {
        return salaryExpectation;
    }

    public void setSalaryExpectation(BigDecimal salaryExpectation) {
        this.salaryExpectation = salaryExpectation;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Boolean getInterestedInJapan() {
        return interestedInJapan;
    }

    public void setInterestedInJapan(Boolean interestedInJapan) {
        this.interestedInJapan = interestedInJapan;
    }

    public String getProjectTypeExperience() {
        return projectTypeExperience;
    }

    public void setProjectTypeExperience(String projectTypeExperience) {
        this.projectTypeExperience = projectTypeExperience;
    }

    public String getLanguageSummary() {
        return languageSummary;
    }

    public void setLanguageSummary(String languageSummary) {
        this.languageSummary = languageSummary;
    }
}

