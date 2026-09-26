package com.vierec.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.Set;

/** Body of {@code PUT /api/v1/users/{id}/roles}: the complete new set of role codes. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AssignRolesRequest")
public class AssignRolesRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "{user.roles.required}")
    @Schema(description = "Replaces every current role", example = "[\"ADMIN\", \"TRAINEE\"]")
    private Set<@NotBlank(message = "{user.roles.blank}") String> roles;
}
