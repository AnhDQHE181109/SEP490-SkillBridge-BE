package com.skillbridge.dto;

public class LoginResponse {
    private String userId;
    private String email;
    private String fullName;
    private String token;

    public LoginResponse() {}

    public LoginResponse(String userId, String email, String fullName, String token) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.token = token;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
