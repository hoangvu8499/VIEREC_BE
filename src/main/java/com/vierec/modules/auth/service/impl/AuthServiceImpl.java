package com.vierec.modules.auth.service.impl;

import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.modules.auth.dto.AuthResponse;
import com.vierec.modules.auth.dto.LoginRequest;
import com.vierec.modules.auth.service.AuthResult;
import com.vierec.modules.auth.service.AuthService;
import com.vierec.modules.user.mapper.UserMapper;
import com.vierec.modules.user.repository.UserRepository;
import com.vierec.security.CustomUserDetails;
import com.vierec.security.CustomUserDetailsService;
import com.vierec.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public AuthResult login(LoginRequest request) {
        // Any failure here (bad password, locked or inactive account) surfaces as an
        // AuthenticationException and is mapped by GlobalExceptionHandler.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword()));

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} logged in", principal.getUsername());
        return issueTokens(principal);
    }

    @Override
    public AuthResult refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Refresh token cookie is missing");
        }
        if (!tokenProvider.validate(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        CustomUserDetails principal;
        try {
            principal = (CustomUserDetails) userDetailsService.loadUserByUsername(
                    tokenProvider.getUsername(refreshToken));
        } catch (UsernameNotFoundException ex) {
            // The account was deleted after the token was issued.
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        // Re-checked on every refresh, so locking or deactivating an account ends its session
        // within one access-token lifetime.
        if (!principal.isAccountNonLocked() || !principal.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        return issueTokens(principal);
    }

    private AuthResult issueTokens(CustomUserDetails principal) {
        AuthResponse body = AuthResponse.builder()
                .expiresIn(tokenProvider.getAccessTokenValiditySeconds())
                .refreshExpiresIn(tokenProvider.getRefreshTokenValiditySeconds())
                .user(userRepository.findById(principal.getId()).map(userMapper::toResponse).orElse(null))
                .build();
        return new AuthResult(tokenProvider.generateAccessToken(principal),
                tokenProvider.generateRefreshToken(principal), body);
    }
}
