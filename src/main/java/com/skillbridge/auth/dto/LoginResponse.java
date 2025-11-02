package com.skillbridge.auth.dto;

public class LoginResponse {
    private String userId;
    private String username;
    private String displayName;
    private String token; // mock token

    public LoginResponse() {}

    public LoginResponse(String userId, String username, String displayName, String token) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.token = token;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
