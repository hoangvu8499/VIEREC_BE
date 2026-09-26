package com.vierec.security;

import com.vierec.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Issues and validates the HS256 JWTs used for stateless authentication.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_AUTHORITIES = "auth";
    private static final String CLAIM_TOKEN_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UserDetails userDetails) {
        String authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        return buildToken(userDetails.getUsername(), authorities, TYPE_ACCESS,
                properties.getAccessTokenValidityMs());
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), null, TYPE_REFRESH,
                properties.getRefreshTokenValidityMs());
    }

    private String buildToken(String subject, String authorities, String tokenType, long validityMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMs);

        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(properties.getIssuer())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .claim(CLAIM_AUTHORITIES, authorities)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("JWT expired: {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT rejected: {}", ex.getMessage());
        }
        return false;
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(parse(token).getBody().get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parse(token).getBody().get(CLAIM_TOKEN_TYPE, String.class));
    }

    public String getUsername(String token) {
        return parse(token).getBody().getSubject();
    }

    public List<String> getAuthorities(String token) {
        String raw = parse(token).getBody().get(CLAIM_AUTHORITIES, String.class);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        return java.util.Arrays.asList(raw.split(","));
    }

    public long getAccessTokenValiditySeconds() {
        return properties.getAccessTokenValidityMs() / 1000L;
    }

    public long getRefreshTokenValiditySeconds() {
        return properties.getRefreshTokenValidityMs() / 1000L;
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseClaimsJws(token);
    }
}
