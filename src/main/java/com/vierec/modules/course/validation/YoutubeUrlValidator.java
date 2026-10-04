package com.vierec.modules.course.validation;

import com.vierec.modules.course.entity.CourseVideos;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class YoutubeUrlValidator implements ConstraintValidator<YoutubeUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.trim().isEmpty() || CourseVideos.youtubeVideoId(value) != null;
    }
}
