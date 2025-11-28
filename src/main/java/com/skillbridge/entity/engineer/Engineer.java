package com.skillbridge.entity.engineer;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "engineers")
public class Engineer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Column(length = 32)
    private String seniority;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(length = 128)
    private String location;

    @Column(name = "language_summary", length = 64)
    private String languageSummary;

    @Column(length = 32)
    private String status;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "salary_expectation", precision = 10, scale = 2)
    private BigDecimal salaryExpectation;

    @Column(name = "primary_skill")
    private String primarySkill;

    @Column(length = 255, unique = true)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 16)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "interested_in_japan")
    private Boolean interestedInJapan;

    @Column(name = "project_type_experience", length = 500)
    private String projectTypeExperience;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLanguageSummary() {
        return languageSummary;
    }

    public void setLanguageSummary(String languageSummary) {
        this.languageSummary = languageSummary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getPrimarySkill() {
        return primarySkill;
    }

    public void setPrimarySkill(String primarySkill) {
        this.primarySkill = primarySkill;
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
}

