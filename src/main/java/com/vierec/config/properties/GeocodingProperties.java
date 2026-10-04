package com.vierec.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Bound from {@code app.geocoding.*}: OpenStreetMap Nominatim, used to find support points around an address.
 * See https://operations.osmfoundation.org/policies/nominatim/ (real User-Agent, at most 1 request per second).
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.geocoding")
public class GeocodingProperties {

    @NotBlank
    private String baseUrl = "https://nominatim.openstreetmap.org/search";

    /** Identifies the application to Nominatim; requests without one are refused. */
    @NotBlank
    private String userAgent = "VIEREC-Academy/1.0";

    /** ISO 3166-1 alpha-2 codes the search is limited to, comma separated. */
    @NotBlank
    private String countryCodes = "vn";

    @NotNull
    private Duration timeout = Duration.ofSeconds(5);
}
