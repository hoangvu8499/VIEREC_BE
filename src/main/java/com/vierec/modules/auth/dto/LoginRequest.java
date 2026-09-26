package com.vierec.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LoginRequest")
public class LoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "{auth.username.required}")
    @Schema(example = "admin", description = "Username or email")
    private String username;

    @NotBlank(message = "{auth.password.required}")
    @Schema(example = "Admin@123")
    private String password;
}
