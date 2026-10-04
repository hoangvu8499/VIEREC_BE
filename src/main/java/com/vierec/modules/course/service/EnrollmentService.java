package com.vierec.modules.course.service;

import com.vierec.common.dto.PageResponse;
import com.vierec.modules.course.dto.EnrollmentResponse;
import com.vierec.modules.course.dto.MonthlyRevenueResponse;
import com.vierec.modules.course.dto.RevenueResponse;
import com.vierec.modules.course.entity.EnrollmentStatus;
import com.vierec.modules.user.entity.User;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface EnrollmentService {

    /**
     * The caller asks to join a PUBLISHED course after paying: the enrollment starts PENDING until an admin approves
     * it. A CANCELLED enrollment is taken up again instead of adding a row (one row per user and course).
     */
    EnrollResult enroll(Long courseId, String username);

    /**
     * Same as {@link #enroll} for users who are not the caller (a business enrolling its learners), in a fixed number
     * of queries whatever the number of users. Users whose enrollment is not CANCELLED are skipped, not an error.
     */
    GroupEnrollResult enrollAll(Long courseId, List<User> users);

    /** Sets the caller's enrollment to CANCELLED; the row is kept. A COMPLETED enrollment cannot be cancelled. */
    void cancel(Long courseId, String username);

    /**
     * The caller's enrollments, newest first.
     *
     * @param status null for PENDING, ENROLLED and COMPLETED (CANCELLED ones are hidden unless asked for)
     */
    PageResponse<EnrollmentResponse> listMine(String username, EnrollmentStatus status, int page, int size);

    /** Enrollments of one course, newest first, for admins. {@code status} null means every status. */
    PageResponse<EnrollmentResponse> listByCourse(Long courseId, EnrollmentStatus status, int page, int size);

    /**
     * Enrollments of every course for admins, newest first.
     *
     * @param status  null means every status
     * @param keyword matches username, full name, phone number or course name; blank means no filter
     */
    PageResponse<EnrollmentResponse> search(EnrollmentStatus status, String keyword, int page, int size);

    /** Admin sets the status; COMPLETED stamps {@code completed_at}. Locked once a certificate was issued. */
    EnrollmentResponse updateStatus(Long courseId, Long enrollmentId, EnrollmentStatus status);

    /** Revenue of approved enrollments in the week (from Monday), month and year that contain {@code today}. */
    RevenueResponse revenue(LocalDate today);

    /**
     * Enrollments approved in {@code month} and still ENROLLED or COMPLETED: totals, per course, and a page of who
     * paid what, latest approval first.
     *
     * @param keyword filters the page and {@code matchedAmount} only (username, full name, phone number or course
     *                name); blank means no filter
     */
    MonthlyRevenueResponse monthlyRevenue(YearMonth month, String keyword, int page, int size);
}
