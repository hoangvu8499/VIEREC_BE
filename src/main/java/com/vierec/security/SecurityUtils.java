package com.vierec.security;

import com.vierec.modules.role.entity.RoleCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** Helpers for code that must branch on the caller instead of just allowing or denying a request. */
public final class SecurityUtils {

    private static final String ADMIN_AUTHORITY = "ROLE_" + RoleCode.ADMIN;
    private static final String SUPER_ADMIN_AUTHORITY = "ROLE_" + RoleCode.SUPER_ADMIN;

    private SecurityUtils() {
    }

    /** True for SUPER_ADMIN / ADMIN; {@code authentication} is null for anonymous callers of public endpoints. */
    public static boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String name = authority.getAuthority();
            if (ADMIN_AUTHORITY.equals(name) || SUPER_ADMIN_AUTHORITY.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** Username of a logged-in caller, or null for anonymous callers of public endpoints. */
    public static String usernameOf(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
