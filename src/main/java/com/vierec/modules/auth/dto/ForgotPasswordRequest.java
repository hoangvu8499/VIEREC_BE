package com.vierec.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Schema(name = "ForgotPasswordRequest")
public class ForgotPasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "{user.email.required}")
    @Email(regexp = RegisterRequest.EMAIL_REGEX, message = "{user.email.invalid}")
    @Size(max = 255, message = "{user.email.size}")
    @Schema(example = "nguyenvana@vierec.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
}
