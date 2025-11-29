package com.skillbridge.dto.admin.request;

/**
 * User List Request DTO
 * Request DTO for paginated user list with search and filter
 */
public class UserListRequest {
    private int page = 0;
    private int pageSize = 10;
    private String search;
    private String role; // SALES_MANAGER, SALES_REP
    private String status; // active, deleted

    // Constructors
    public UserListRequest() {
    }

    public UserListRequest(int page, int pageSize, String search, String role, String status) {
        this.page = page;
        this.pageSize = pageSize;
        this.search = search;
        this.role = role;
        this.status = status;
    }

    // Getters and Setters
    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

