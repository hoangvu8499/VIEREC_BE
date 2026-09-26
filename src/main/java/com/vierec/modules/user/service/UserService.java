package com.vierec.modules.user.service;

import com.vierec.common.dto.PageResponse;
import com.vierec.modules.auth.dto.RegisterRequest;
import com.vierec.modules.user.dto.AssignRolesRequest;
import com.vierec.modules.user.dto.CreateUserRequest;
import com.vierec.modules.user.dto.UpdateUserRequest;
import com.vierec.modules.user.dto.UserResponse;
import com.vierec.modules.user.entity.UserStatus;
import org.springframework.data.domain.Pageable;

public interface UserService {

    /** Self-registration: creates an ACTIVE account with the TRAINEE role. */
    UserResponse register(RegisterRequest request);

    /** Admin-created account with the given roles; {@code status} defaults to ACTIVE. */
    UserResponse create(CreateUserRequest request, String actorUsername);

    UserResponse getById(Long id);

    UserResponse getByUsername(String username);

    PageResponse<UserResponse> search(String keyword, UserStatus status, Pageable pageable);

    UserResponse update(Long id, UpdateUserRequest request);

    /**
     * Replaces the roles of a user. Only a SUPER_ADMIN may grant or remove SUPER_ADMIN or touch the roles of a
     * SUPER_ADMIN, and nobody may change their own roles.
     */
    UserResponse assignRoles(Long id, AssignRolesRequest request, String actorUsername);

    /** Soft delete: sets {@code deleted_at}; the row stays but is filtered out of every read. */
    void delete(Long id);
}
