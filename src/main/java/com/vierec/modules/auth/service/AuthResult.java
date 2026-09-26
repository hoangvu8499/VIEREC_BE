package com.vierec.modules.auth.service;

import com.vierec.modules.auth.dto.AuthResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * What the auth service hands back to the controller: the tokens go into cookies, only {@link #body} is
 * serialized. Never return this object from a controller.
 */
@Getter
@AllArgsConstructor
public class AuthResult {

    private final String accessToken;
    private final String refreshToken;
    private final AuthResponse body;
}
