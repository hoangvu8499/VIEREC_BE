package com.vierec.modules.course.entity;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Values of the {@code course_enrollments.status} ENUM column.
 */
public enum EnrollmentStatus {

    /** Learner reported a bank transfer; an admin approves (ENROLLED) or rejects (CANCELLED) it. */
    PENDING,
    ENROLLED,
    COMPLETED,
    CANCELLED;

    /** Statuses that open the lesson videos and files (PENDING still waits for the payment to be checked). */
    public static final Set<EnrollmentStatus> LEARNING = Collections.unmodifiableSet(EnumSet.of(ENROLLED, COMPLETED));
}
