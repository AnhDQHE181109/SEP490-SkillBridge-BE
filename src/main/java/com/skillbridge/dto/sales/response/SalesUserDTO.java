package com.skillbridge.dto.sales.response;

/**
 * Sales User DTO
 * Data transfer object for sales user information
 */
public class SalesUserDTO {
    private Integer id;
    private String email;
    private String fullName;
    private String role;

    // Constructors
    public SalesUserDTO() {
    }

    public SalesUserDTO(Integer id, String email, String fullName, String role) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

