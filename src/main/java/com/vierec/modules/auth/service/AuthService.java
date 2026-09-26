package com.vierec.modules.auth.service;

import com.vierec.modules.auth.dto.LoginRequest;

public interface AuthService {

    AuthResult login(LoginRequest request);

    /** Issues a new token pair (refresh token rotation) from a valid refresh token. */
    AuthResult refresh(String refreshToken);
}
