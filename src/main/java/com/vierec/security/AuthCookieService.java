package com.vierec.security;

import com.vierec.config.properties.CookieProperties;
import com.vierec.config.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

/**
 * Reads and writes the auth token cookies.
 *
 * <p>Both cookies are {@code HttpOnly}, so page scripts (and therefore XSS) cannot read the tokens.
 * Their {@code Max-Age} matches the token lifetime, so the browser drops a cookie when its token expires.</p>
 */
@Component
@RequiredArgsConstructor
public class AuthCookieService {

    private static final String ACCESS_TOKEN_PATH = "/";

    private final CookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    public void writeTokens(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(cookieProperties.getAccessTokenName(), accessToken,
                ACCESS_TOKEN_PATH, Duration.ofMillis(jwtProperties.getAccessTokenValidityMs())).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, build(cookieProperties.getRefreshTokenName(), refreshToken,
                cookieProperties.getRefreshTokenPath(),
                Duration.ofMillis(jwtProperties.getRefreshTokenValidityMs())).toString());
    }

    /**
     * Expires both cookies ({@code Max-Age=0}). Name, path and domain must match the ones used when writing,
     * otherwise the browser treats it as a different cookie and keeps the original.
     */
    public void clearTokens(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                build(cookieProperties.getAccessTokenName(), "", ACCESS_TOKEN_PATH, Duration.ZERO).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, build(cookieProperties.getRefreshTokenName(), "",
                cookieProperties.getRefreshTokenPath(), Duration.ZERO).toString());
    }

    public String readAccessToken(HttpServletRequest request) {
        return read(request, cookieProperties.getAccessTokenName());
    }

    public String readRefreshToken(HttpServletRequest request) {
        return read(request, cookieProperties.getRefreshTokenName());
    }

    private ResponseCookie build(String name, String value, String path, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(path)
                .maxAge(maxAge);
        if (StringUtils.hasText(cookieProperties.getDomain())) {
            builder.domain(cookieProperties.getDomain());
        }
        return builder.build();
    }

    private static String read(HttpServletRequest request, String name) {
        Cookie cookie = WebUtils.getCookie(request, name);
        return cookie != null && StringUtils.hasText(cookie.getValue()) ? cookie.getValue() : null;
    }
}
