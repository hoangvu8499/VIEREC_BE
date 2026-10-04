package com.vierec.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/** The new password follows the registration rules. */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"code", "newPassword"})
@Schema(name = "ResetPasswordRequest")
public class ResetPasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "{user.email.required}")
    @Email(regexp = RegisterRequest.EMAIL_REGEX, message = "{user.email.invalid}")
    @Size(max = 255, message = "{user.email.size}")
    @Schema(example = "nguyenvana@vierec.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "{auth.resetCode.required}")
    @Pattern(regexp = "^[0-9]{6}$", message = "{auth.resetCode.pattern}")
    @Schema(description = "6-digit code from the email", example = "482913",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotBlank(message = "{user.password.required}")
    @Size(min = 8, max = 72, message = "{user.password.size}")
    @Pattern(regexp = RegisterRequest.PASSWORD_REGEX, message = "{user.password.special}")
    @Schema(example = "Matkhau@456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
