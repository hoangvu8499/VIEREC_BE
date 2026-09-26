package com.vierec.modules.auth.dto;

import com.vierec.modules.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Body of a successful login / refresh. The tokens themselves are only in the HttpOnly cookies.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AuthResponse", description = "Logged-in user; the tokens are set as HttpOnly cookies")
public class AuthResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Access token lifetime in seconds. */
    @Schema(example = "1800")
    private long expiresIn;

    /** Refresh token lifetime in seconds. */
    @Schema(example = "2592000")
    private long refreshExpiresIn;

    private UserResponse user;
}
