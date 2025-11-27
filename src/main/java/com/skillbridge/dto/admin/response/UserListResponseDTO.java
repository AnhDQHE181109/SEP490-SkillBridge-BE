package com.skillbridge.dto.admin.response;

import java.util.List;

/**
 * User List Response DTO
 * Response DTO for paginated user list in Admin User List
 */
public class UserListResponseDTO {
    private List<UserResponseDTO> users;
    private PageInfo page;

    // Constructors
    public UserListResponseDTO() {
    }

    public UserListResponseDTO(List<UserResponseDTO> users, PageInfo page) {
        this.users = users;
        this.page = page;
    }

    // Getters and Setters
    public List<UserResponseDTO> getUsers() {
        return users;
    }

    public void setUsers(List<UserResponseDTO> users) {
        this.users = users;
    }

    public PageInfo getPage() {
        return page;
    }

    public void setPage(PageInfo page) {
        this.page = page;
    }
}

