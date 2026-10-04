package com.vierec.modules.user.dto;

import com.vierec.modules.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/** The new password follows the registration rules. */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"currentPassword", "newPassword"})
@Schema(name = "ChangePasswordRequest")
public class ChangePasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "{user.currentPassword.required}")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String currentPassword;

    @NotBlank(message = "{user.password.required}")
    @Size(min = 8, max = 72, message = "{user.password.size}")
    @Pattern(regexp = RegisterRequest.PASSWORD_REGEX, message = "{user.password.special}")
    @Schema(example = "Matkhau@456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
