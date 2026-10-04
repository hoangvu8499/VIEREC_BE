package com.vierec.modules.role.entity;

/**
 * Codes of the rows in the {@code roles} table. Spring Security sees them as {@code ROLE_<code>},
 * so {@code hasRole('ADMIN')} matches {@link #ADMIN}.
 */
public final class RoleCode {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ADMIN = "ADMIN";
    public static final String TRAINEE = "TRAINEE";
    /** Manager of a corporate customer: creates, enrolls and follows the business's learners. */
    public static final String BUSINESS = "BUSINESS";

    /** Role given to every self-registered account. */
    public static final String DEFAULT_FOR_REGISTRATION = TRAINEE;

    private RoleCode() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}
