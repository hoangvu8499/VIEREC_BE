package com.vierec.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * Bound from {@code app.cookie.*}: how the auth tokens are written to cookies.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.cookie")
public class CookieProperties {

    @NotBlank
    private String accessTokenName = "access_token";

    @NotBlank
    private String refreshTokenName = "refresh_token";

    /** The access cookie goes with every request; the refresh cookie only with the auth endpoints. */
    @NotBlank
    private String refreshTokenPath = "/api/v1/auth";

    /** Send cookies over HTTPS only. Must be true in every environment served over HTTPS. */
    private boolean secure;

    /** {@code SameSite=None} also requires {@code secure=true}, otherwise browsers drop the cookie. */
    @Pattern(regexp = "Strict|Lax|None")
    private String sameSite = "Lax";

    /** Empty = host-only cookie. */
    private String domain;
}
