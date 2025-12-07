package com.skillbridge.dto.admin.request;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Create Engineer Request DTO
 * Request body for creating a new engineer with comprehensive information
 */
public class CreateEngineerRequest {

    // Basic Info
    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Size(max = 16, message = "Gender must not exceed 16 characters")
    private String gender; // Male, Female, Other

    private LocalDate dateOfBirth;

    @NotBlank(message = "Seniority is required")
    @Pattern(regexp = "Junior|Mid-level|Senior|Lead", message = "Seniority must be one of: Junior, Mid-level, Senior, Lead")
    private String seniority; // Level field in form maps to seniority

    private String introduction; // Introduce field in form

    // Professional Info
    @NotNull(message = "Years of experience is required")
    @Min(value = 0, message = "Years of experience must be at least 0")
    @Max(value = 100, message = "Years of experience must not exceed 100")
    private Integer yearsExperience;

    @Size(max = 128, message = "Primary skill must not exceed 128 characters")
    private String primarySkill; // Comma-separated string

    private List<Integer> otherSkills; // Array of skill IDs for engineer_skills table

    private String projectTypeExperience; // Comma-separated string of project type IDs or names

    // Foreign Language
    @Size(max = 64, message = "Language summary must not exceed 64 characters")
    private String languageSummary;

    // Other
    @DecimalMin(value = "0.0", message = "Salary expectation must be at least 0")
    @DecimalMax(value = "999999999.99", message = "Salary expectation must not exceed 999999999.99")
    private BigDecimal salaryExpectation;

    private Boolean interestedInJapan;

    // General
    @Size(max = 128, message = "Location must not exceed 128 characters")
    private String location;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "AVAILABLE|BUSY|UNAVAILABLE", message = "Status must be one of: AVAILABLE, BUSY, UNAVAILABLE")
    private String status;

    private String summary;

    @Size(max = 500, message = "Profile image URL must not exceed 500 characters")
    private String profileImageUrl;

    // Certificates
    @Valid
    private List<CertificateRequest> certificates;

    // Constructors
    public CreateEngineerRequest() {
    }

    // Getters and Setters
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

    public List<Integer> getOtherSkills() {
        return otherSkills;
    }

    public void setOtherSkills(List<Integer> otherSkills) {
        this.otherSkills = otherSkills;
    }

    public List<CertificateRequest> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<CertificateRequest> certificates) {
        this.certificates = certificates;
    }

    public String getLanguageSummary() {
        return languageSummary;
    }

    public void setLanguageSummary(String languageSummary) {
        this.languageSummary = languageSummary;
    }
}

