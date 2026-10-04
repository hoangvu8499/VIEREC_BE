package com.vierec.modules.course.service;

import com.vierec.modules.course.dto.EnrollmentResponse;
import com.vierec.modules.course.entity.CourseEnrollment;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Result of enrolling several users at once: the requests sent now and the users who already had one. */
@Getter
@AllArgsConstructor
public class GroupEnrollResult {

    private final List<EnrollmentResponse> enrolled;

    /** Enrollments left untouched because they are PENDING, ENROLLED or COMPLETED. */
    private final List<CourseEnrollment> skipped;
}
