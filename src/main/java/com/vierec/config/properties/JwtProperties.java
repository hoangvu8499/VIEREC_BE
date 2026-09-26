package com.vierec.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

/**
 * Bound from {@code app.jwt.*}. Validated at startup so a misconfigured environment
 * fails fast instead of at the first login attempt.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Base64 or raw secret. HS256 requires at least 256 bits (32 chars). */
    @NotBlank
    @Size(min = 32, message = "app.jwt.secret must be at least 32 characters for HS256")
    private String secret;

    /** Access token lifetime in milliseconds. */
    @Positive
    private long accessTokenValidityMs = 3_600_000L;

    /** Refresh token lifetime in milliseconds. */
    @Positive
    private long refreshTokenValidityMs = 604_800_000L;

    private String issuer = "vierec-be";
}
