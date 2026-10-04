package com.vierec.modules.auth.service;

/**
 * Locks an account after {@link #MAX_FAILURES} wrong passwords in a row; an admin unlocks it. A SUPER_ADMIN is only
 * blocked for {@link #SUPER_ADMIN_BLOCK_MINUTES} minutes, so that strangers typing wrong passwords cannot lock out
 * the only accounts able to unlock the others.
 */
public interface LoginAttemptService {

    int MAX_FAILURES = 5;
    int SUPER_ADMIN_BLOCK_MINUTES = 15;

    /** Throws {@code LOGIN_TEMPORARILY_BLOCKED} while a SUPER_ADMIN is blocked. Unknown names pass. */
    void checkNotBlocked(String usernameOrEmail);

    /** Counts a wrong password; returns true when the account got locked by it. Unknown names are ignored. */
    boolean recordFailure(String usernameOrEmail);

    void recordSuccess(Long userId);
}
