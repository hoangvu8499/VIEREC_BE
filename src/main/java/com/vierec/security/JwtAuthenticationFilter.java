package com.vierec.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reads the access token from its cookie and populates the {@link SecurityContextHolder}.
 *
 * <p>Only tokens of type {@code access} authenticate a request: a refresh token (30-day lifetime) must never
 * work as an access token, even if a client puts it in the access cookie.</p>
 *
 * <p>Authorities are taken from the token claim, so the happy path does not hit the database.
 * A user whose roles changed keeps the old ones until their access token expires (at most 30 minutes) - an
 * accepted trade-off for stateless auth with short-lived tokens.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final AuthCookieService authCookieService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = authCookieService.readAccessToken(request);

        if (StringUtils.hasText(token) && tokenProvider.validate(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (tokenProvider.isAccessToken(token)) {
                    authenticate(request, token);
                } else {
                    log.debug("Rejected a non-access token presented as access token");
                }
            } catch (Exception ex) {
                // A bad token must not break the chain: the request simply stays anonymous
                // and the entry point turns it into a 401.
                log.debug("Could not authenticate request: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        String username = tokenProvider.getUsername(token);
        List<SimpleGrantedAuthority> authorities = tokenProvider.getAuthorities(token).stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
