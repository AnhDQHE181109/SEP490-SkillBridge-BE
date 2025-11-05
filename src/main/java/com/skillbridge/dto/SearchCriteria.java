package com.skillbridge.dto;

import java.math.BigDecimal;
import java.util.List;

public class SearchCriteria {

    private String query;
    private List<String> skills;
    private Integer experienceMin;
    private Integer experienceMax;
    private List<String> seniority;
    private List<String> location;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private Boolean availability;
    private Integer page;
    private Integer size;
    private String sortBy;

    public SearchCriteria() {
        this.page = 0;
        this.size = 20;
        this.sortBy = "relevance";
        this.availability = true;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public Integer getExperienceMin() {
        return experienceMin;
    }

    public void setExperienceMin(Integer experienceMin) {
        this.experienceMin = experienceMin;
    }

    public Integer getExperienceMax() {
        return experienceMax;
    }

    public void setExperienceMax(Integer experienceMax) {
        this.experienceMax = experienceMax;
    }

    public List<String> getSeniority() {
        return seniority;
    }

    public void setSeniority(List<String> seniority) {
        this.seniority = seniority;
    }

    public List<String> getLocation() {
        return location;
    }

    public void setLocation(List<String> location) {
        this.location = location;
    }

    public BigDecimal getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(BigDecimal salaryMin) {
        this.salaryMin = salaryMin;
    }

    public BigDecimal getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(BigDecimal salaryMax) {
        this.salaryMax = salaryMax;
    }

    public Boolean getAvailability() {
        return availability;
    }

    public void setAvailability(Boolean availability) {
        this.availability = availability;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    @Override
    public String toString() {
        return "SearchCriteria{" +
                "query='" + query + '\'' +
                ", skills=" + skills +
                ", experienceMin=" + experienceMin +
                ", experienceMax=" + experienceMax +
                ", seniority=" + seniority +
                ", location=" + location +
                ", salaryMin=" + salaryMin +
                ", salaryMax=" + salaryMax +
                ", availability=" + availability +
                ", page=" + page +
                ", size=" + size +
                ", sortBy='" + sortBy + '\'' +
                '}';
    }
}
