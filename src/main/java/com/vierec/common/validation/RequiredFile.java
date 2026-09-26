package com.vierec.common.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The multipart file must be present and non-empty. {@code @NotNull} is not enough: an empty file input
 * still arrives as a part with size 0.
 */
@Documented
@Constraint(validatedBy = RequiredFileValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredFile {

    String message() default "{file.required}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
