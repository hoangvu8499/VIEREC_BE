package com.vierec.modules.course.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A YouTube video link ({@code watch?v=}, {@code youtu.be/}, {@code /embed/}, {@code /shorts/}...); blank means
 * "no video". Lesson videos are YouTube only: the player and the watch progress need the video id.
 */
@Documented
@Constraint(validatedBy = YoutubeUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface YoutubeUrl {

    String message() default "{lesson.videoUrl.youtube}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
