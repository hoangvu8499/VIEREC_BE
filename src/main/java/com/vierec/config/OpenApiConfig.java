package com.vierec.config;

import com.vierec.common.constant.AppConstants;
import com.vierec.config.properties.CookieProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    private final CookieProperties cookieProperties;

    @Value("${app.info.version:1.0.0}")
    private String version;

    @Bean
    public OpenAPI vierecOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("VIEREC Backend API")
                        .description("REST API for the VIEREC platform")
                        .version(version)
                        .contact(new Contact().name("VIEREC Team").email("dev@vierec.com"))
                        .license(new License().name("Proprietary")))
                .servers(Arrays.asList(
                        new Server().url("http://localhost:8383").description("Local"),
                        new Server().url("https://api.vierec.com").description("Production")))
                // The browser sends the cookie by itself: call /api/v1/auth/login from Swagger UI first.
                .components(new Components().addSecuritySchemes(AppConstants.AUTH_COOKIE_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(cookieProperties.getAccessTokenName())
                                .description("HttpOnly cookie set by POST /api/v1/auth/login")));
    }
}
