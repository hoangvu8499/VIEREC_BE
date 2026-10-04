package com.vierec.security.access;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Back-office endpoints (SUPER_ADMIN, ADMIN). The rule lives here only, so moving an area to a finer permission
 * means a new annotation like this one instead of editing every controller.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public @interface AdminOnly {
}
