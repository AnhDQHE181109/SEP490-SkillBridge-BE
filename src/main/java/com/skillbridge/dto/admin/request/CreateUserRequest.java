package com.skillbridge.dto.admin.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create User Request DTO
 * Request DTO for creating a new user
 */
public class CreateUserRequest {
    
    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "SALES_MANAGER|SALES_REP", message = "Role must be either SALES_MANAGER or SALES_REP")
    private String role;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    @Pattern(regexp = "^[0-9-]*$", message = "Phone number can only contain numbers and dashes")
    private String phone;

    // Constructors
    public CreateUserRequest() {
    }

    public CreateUserRequest(String fullName, String role, String email, String phone) {
        this.fullName = fullName;
        this.role = role;
        this.email = email;
        this.phone = phone;
    }

    // Getters and Setters
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
}

