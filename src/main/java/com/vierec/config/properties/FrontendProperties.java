package com.vierec.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

/** Bound from {@code app.frontend.*}: links printed outside the web app (certificate QR code). */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.frontend")
public class FrontendProperties {

    /** Public address of the web app, without a trailing slash. */
    @NotBlank
    private String baseUrl = "http://localhost:8484";
}
