package com.vierec.modules.course.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.common.dto.ApiResponse;
import com.vierec.common.dto.PageResponse;
import com.vierec.modules.course.dto.EnrollmentResponse;
import com.vierec.modules.course.dto.MonthlyRevenueResponse;
import com.vierec.modules.course.dto.RevenueResponse;
import com.vierec.modules.course.dto.UpdateEnrollmentRequest;
import com.vierec.modules.course.entity.EnrollmentStatus;
import com.vierec.modules.course.service.EnrollResult;
import com.vierec.modules.course.service.EnrollmentService;
import com.vierec.security.access.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping(AppConstants.API_V1)
@RequiredArgsConstructor
@Validated
@Tag(name = "Enrollments", description = "Enrolling in courses and tracking completion")
@SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/courses/{courseId}/enrollments")
    @Operation(summary = "Ask to join a PUBLISHED course after paying (status PENDING until an admin approves)",
            description = "201 for a new enrollment, 200 when a CANCELLED enrollment is taken up again.")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enroll(@PathVariable @Positive Long courseId,
                                                                  Authentication authentication) {
        EnrollResult result = enrollmentService.enroll(courseId, authentication.getName());
        return ResponseEntity.status(result.isCreated() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.success(result.getBody(), "Enrolled"));
    }

    @DeleteMapping("/courses/{courseId}/enrollments/me")
    @Operation(summary = "Cancel the logged-in user's enrollment (status becomes CANCELLED)")
    public ResponseEntity<Void> cancel(@PathVariable @Positive Long courseId, Authentication authentication) {
        enrollmentService.cancel(courseId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/auth/me/enrollments")
    @Operation(summary = "Courses of the logged-in user, newest enrollment first",
            description = "Without status: PENDING, ENROLLED and COMPLETED (CANCELLED is only returned when asked "
                    + "for).")
    public ApiResponse<PageResponse<EnrollmentResponse>> mine(
            @RequestParam(required = false) EnrollmentStatus status,
            @Parameter(description = "0-based page number") @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{page.min}") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "{size.range}")
            @Max(value = AppConstants.MAX_PAGE_SIZE, message = "{size.range}") int size,
            Authentication authentication) {
        return ApiResponse.success(enrollmentService.listMine(authentication.getName(), status, page, size));
    }

    @GetMapping("/courses/{courseId}/enrollments")
    @AdminOnly
    @Operation(summary = "Enrollments of a course, newest first")
    public ApiResponse<PageResponse<EnrollmentResponse>> listByCourse(
            @PathVariable @Positive Long courseId,
            @RequestParam(required = false) EnrollmentStatus status,
            @Parameter(description = "0-based page number") @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{page.min}") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "{size.range}")
            @Max(value = AppConstants.MAX_PAGE_SIZE, message = "{size.range}") int size) {
        return ApiResponse.success(enrollmentService.listByCourse(courseId, status, page, size));
    }

    @GetMapping("/enrollments")
    @AdminOnly
    @Operation(summary = "Enrollments of every course, newest first",
            description = "E.g. status=PENDING for the approval queue. keyword matches username, full name, "
                    + "phone number or course name.")
    public ApiResponse<PageResponse<EnrollmentResponse>> search(
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(required = false) String keyword,
            @Parameter(description = "0-based page number") @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{page.min}") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "{size.range}")
            @Max(value = AppConstants.MAX_PAGE_SIZE, message = "{size.range}") int size) {
        return ApiResponse.success(enrollmentService.search(status, keyword, page, size));
    }

    @GetMapping("/enrollments/revenue")
    @AdminOnly
    @Operation(summary = "Revenue of approved enrollments this week (from Monday), month and year",
            description = "Sum of the price snapshot of ENROLLED and COMPLETED enrollments, by approval date.")
    public ApiResponse<RevenueResponse> revenue() {
        return ApiResponse.success(enrollmentService.revenue(LocalDate.now()));
    }

    @GetMapping("/enrollments/revenue/monthly")
    @AdminOnly
    @Operation(summary = "Revenue details of one month: totals, per course, and who paid what",
            description = "Only ENROLLED and COMPLETED enrollments, by approval date. keyword (username, full name, "
                    + "phone number or course name) filters items and matchedAmount; the totals and courses are "
                    + "always the whole month.")
    public ApiResponse<MonthlyRevenueResponse> monthlyRevenue(
            @Parameter(description = "yyyy-MM, default this month", example = "2026-10")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false) String keyword,
            @Parameter(description = "0-based page number") @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{page.min}") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "{size.range}")
            @Max(value = AppConstants.MAX_PAGE_SIZE, message = "{size.range}") int size) {
        return ApiResponse.success(enrollmentService.monthlyRevenue(month == null ? YearMonth.now() : month,
                keyword, page, size));
    }

    @PutMapping("/courses/{courseId}/enrollments/{enrollmentId}")
    @AdminOnly
    @Operation(summary = "Set the status of an enrollment, e.g. COMPLETED",
            description = "Locked once a certificate was issued for the enrollment (VRC-409-402).")
    public ApiResponse<EnrollmentResponse> updateStatus(@PathVariable @Positive Long courseId,
                                                        @PathVariable @Positive Long enrollmentId,
                                                        @Valid @RequestBody UpdateEnrollmentRequest request) {
        return ApiResponse.success(enrollmentService.updateStatus(courseId, enrollmentId, request.getStatus()),
                "Enrollment updated");
    }
}
