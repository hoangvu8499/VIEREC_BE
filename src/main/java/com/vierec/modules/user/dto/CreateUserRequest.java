package com.vierec.modules.user.dto;

import com.vierec.modules.auth.dto.RegisterRequest;
import com.vierec.modules.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.Set;

/**
 * Body of {@code POST /api/v1/users}: the registration fields (same rules, all required) plus the roles
 * and an optional status.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@Schema(name = "CreateUserRequest", description = "Admin-created account")
public class CreateUserRequest extends RegisterRequest {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "{user.roles.required}")
    @Schema(description = "Role codes, see GET /api/v1/roles", example = "[\"TRAINEE\"]")
    private Set<@NotBlank(message = "{user.roles.blank}") String> roles;

    @Schema(description = "Defaults to ACTIVE", example = "ACTIVE")
    private UserStatus status;
}
