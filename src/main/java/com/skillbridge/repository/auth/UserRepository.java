package com.skillbridge.repository.auth;

import com.skillbridge.entity.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User Repository
 * Handles database operations for users
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Find user by email
     * @param email User email
     * @return Optional User
     */
    Optional<User> findByEmail(String email);

    /**
     * Find users by role
     * @param role User role
     * @return List of users with the specified role
     */
    @Query("SELECT u FROM com.skillbridge.entity.auth.User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") String role);

    /**
     * Check if email exists
     * @param email User email
     * @return true if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Search users by name, email, or phone with pagination
     * @param searchTerm Search term for name, email, or phone
     * @param pageable Pagination information
     * @return Page of users
     */
    @Query("SELECT u FROM com.skillbridge.entity.auth.User u WHERE " +
           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find users with role and status filter, with search
     * @param searchTerm Search term (optional)
     * @param role User role (optional)
     * @param isActive Active status (optional)
     * @param pageable Pagination information
     * @return Page of users
     */
    @Query("SELECT u FROM com.skillbridge.entity.auth.User u WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:isActive IS NULL OR u.isActive = :isActive) " +
           "ORDER BY u.createdAt DESC")
    Page<User> findUsersWithFilters(@Param("searchTerm") String searchTerm,
                                    @Param("role") String role,
                                    @Param("isActive") Boolean isActive,
                                    Pageable pageable);

    /**
     * Count users by active status
     * @param isActive Active status (true for active, false for inactive)
     * @return Count of users with the specified active status
     */
    Long countByIsActive(Boolean isActive);
}

