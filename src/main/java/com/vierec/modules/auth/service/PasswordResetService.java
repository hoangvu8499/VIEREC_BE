package com.vierec.modules.auth.service;

/** Forgotten password: a 6-digit code is emailed, then the code and a new password are sent back. */
public interface PasswordResetService {

    int CODE_VALID_MINUTES = 10;
    int MAX_ATTEMPTS = 5;
    /** A new code is not sent sooner than this after the previous one. */
    int RESEND_AFTER_SECONDS = 60;

    /**
     * Emails a new code to the account with this email, replacing its previous code. Unknown emails, and requests
     * within {@link #RESEND_AFTER_SECONDS} of the previous code, are ignored silently: the answer never tells
     * whether an account exists.
     */
    void sendCode(String email);

    /**
     * Sets the new password when the code is the account's current one, not expired, and typed right within
     * {@link #MAX_ATTEMPTS} tries; otherwise {@code RESET_CODE_INVALID}. A locked account stays locked.
     */
    void resetPassword(String email, String code, String newPassword);
}
