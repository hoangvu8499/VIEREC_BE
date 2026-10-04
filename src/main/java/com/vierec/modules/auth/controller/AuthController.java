package com.vierec.modules.auth.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.common.dto.ApiResponse;
import com.vierec.modules.auth.dto.AuthResponse;
import com.vierec.modules.auth.dto.ForgotPasswordRequest;
import com.vierec.modules.auth.dto.LoginRequest;
import com.vierec.modules.auth.dto.RegisterRequest;
import com.vierec.modules.auth.dto.ResetPasswordRequest;
import com.vierec.modules.auth.service.AuthResult;
import com.vierec.modules.auth.service.AuthService;
import com.vierec.modules.auth.service.PasswordResetService;
import com.vierec.modules.user.dto.ChangePasswordRequest;
import com.vierec.modules.user.dto.UpdateProfileRequest;
import com.vierec.modules.user.dto.UserResponse;
import com.vierec.modules.user.service.UserService;
import com.vierec.security.AuthCookieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@RestController
@RequestMapping(AppConstants.API_V1 + "/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login and token lifecycle")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final AuthCookieService authCookieService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @Operation(summary = "Register a new trainee account")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userService.register(request), "Registration successful"));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with username or email",
            description = "Sets the HttpOnly cookies access_token (30 minutes) and refresh_token (30 days).")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResult result = authService.login(request);
        authCookieService.writeTokens(response, result.getAccessToken(), result.getRefreshToken());
        return ApiResponse.success(result.getBody(), "Login successful");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Email a 6-digit code to reset the password",
            description = "Always 200, whether the email belongs to an account or not. A new code replaces the "
                    + "previous one; asking again within 60 seconds sends nothing. The code is valid 10 minutes.")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.sendCode(request.getEmail());
        return ApiResponse.success(null, "If the email belongs to an account, a code was sent to it");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set a new password with the emailed code",
            description = "RESET_CODE_INVALID when the code is wrong, expired, replaced or tried 5 times. "
                    + "A locked account stays locked until an admin unlocks it.")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ApiResponse.success(null, "Password changed");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Get a new token pair from the refresh_token cookie",
            description = "Both cookies are replaced; the old refresh token keeps working until it expires.")
    public ApiResponse<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        AuthResult result = authService.refresh(authCookieService.readRefreshToken(request));
        authCookieService.writeTokens(response, result.getAccessToken(), result.getRefreshToken());
        return ApiResponse.success(result.getBody(), "Token refreshed");
    }

    /**
     * Public on purpose: a user whose access token already expired must still be able to log out.
     * The tokens are not revoked server-side (no revocation table), only removed from the browser.
     */
    @PostMapping("/logout")
    @Operation(summary = "Log out", description = "Removes the access_token and refresh_token cookies.")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        authCookieService.clearTokens(response);
        return ApiResponse.success("Logout successful");
    }

    @GetMapping("/me")
    @Operation(summary = "Profile of the currently logged-in user")
    @SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
    public ApiResponse<UserResponse> me(Authentication authentication) {
        return ApiResponse.success(userService.getByUsername(authentication.getName()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the profile of the currently logged-in user",
            description = "Omitted fields are left unchanged; status, roles, username and password are ignored. "
                    + "Once a certificate was issued, name, date of birth and CCCD are locked (VRC-409-105).")
    @SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
    public ApiResponse<UserResponse> updateMe(@Valid @RequestBody UpdateProfileRequest request,
                                              Authentication authentication) {
        return ApiResponse.success(userService.updateProfile(authentication.getName(), request), "Profile updated");
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change the password of the currently logged-in user",
            description = "Existing tokens stay valid until they expire.")
    @SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               Authentication authentication) {
        userService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
