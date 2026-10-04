package com.vierec.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

/** Bound from {@code app.mail.*}; the SMTP server itself is {@code spring.mail.*}. */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.mail")
public class AppMailProperties {

    /** Sender shown to the recipient, e.g. {@code VIEREC Academy <no-reply@vierec.vn>}. */
    @NotBlank
    private String from = "VIEREC Academy <no-reply@vierec.vn>";
}
