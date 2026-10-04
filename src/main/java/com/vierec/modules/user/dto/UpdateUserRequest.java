package com.vierec.modules.user.dto;

import com.vierec.modules.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Admin update of a user: the personal fields of {@link UpdateProfileRequest} plus the account status.
 * Roles are changed through {@code PUT /api/v1/users/{id}/roles}.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "UpdateUserRequest", description = "Fields to update; omit a field to leave it unchanged")
public class UpdateUserRequest extends UpdateProfileRequest {

    private static final long serialVersionUID = 1L;

    private UserStatus status;
}
