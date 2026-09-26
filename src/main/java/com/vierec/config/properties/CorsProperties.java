package com.vierec.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * Bound from {@code app.cors.*}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<String> allowedOrigins = Arrays.asList("http://localhost:8484");
    private List<String> allowedMethods = Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = Arrays.asList("*");
    private List<String> exposedHeaders = Arrays.asList("X-Trace-Id");
    private boolean allowCredentials = true;
    private long maxAge = 3600L;
}
