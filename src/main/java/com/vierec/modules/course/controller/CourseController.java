package com.vierec.modules.course.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.common.dto.ApiResponse;
import com.vierec.common.dto.PageResponse;
import com.vierec.modules.course.dto.CourseDetailResponse;
import com.vierec.modules.course.dto.CourseResponse;
import com.vierec.modules.course.dto.CourseRequest;
import com.vierec.modules.course.entity.CourseStatus;
import com.vierec.modules.course.service.CourseService;
import com.vierec.security.SecurityUtils;
import com.vierec.security.access.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;

@RestController
@RequestMapping(AppConstants.API_V1 + "/courses")
@RequiredArgsConstructor
@Validated
@Tag(name = "Courses", description = "Course list and course administration")
@SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "List courses, 10 per page, newest first (no login required)",
            description = "SUPER_ADMIN / ADMIN see every status and may filter by status; "
                    + "anonymous and other users only see PUBLISHED courses. "
                    + "myEnrollmentStatus is the caller's enrollment status (null when not enrolled or anonymous).")
    public ApiResponse<PageResponse<CourseResponse>> list(
            @Parameter(description = "0-based page number") @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{page.min}") int page,
            @Parameter(description = "Part of the course name") @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CourseStatus status,
            @Parameter(description = "true leaves out the courses the caller is ENROLLED in or COMPLETED "
                    + "(PENDING ones stay); ignored when anonymous")
            @RequestParam(defaultValue = "false") boolean excludeLearning,
            @Parameter(hidden = true) Authentication authentication) {
        return ApiResponse.success(courseService.list(keyword, status, page, !SecurityUtils.isAdmin(authentication),
                SecurityUtils.usernameOf(authentication), excludeLearning));
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Course detail with its lessons (no login required)",
            description = "Anonymous and non-admin users get 404 for courses that are not PUBLISHED. "
                    + "Unless the caller is an admin or ENROLLED / COMPLETED, lessons come without "
                    + "documentUrl, videoUrl and files.")
    public ApiResponse<CourseDetailResponse> get(@PathVariable @Positive Long id,
                                                 @Parameter(hidden = true) Authentication authentication) {
        return ApiResponse.success(courseService.get(id, !SecurityUtils.isAdmin(authentication),
                SecurityUtils.usernameOf(authentication)));
    }

    @PostMapping
    @AdminOnly
    @Operation(summary = "Create a course (every field is required)")
    public ResponseEntity<ApiResponse<CourseResponse>> create(@Valid @RequestBody CourseRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(courseService.create(request, authentication.getName()),
                        "Course created"));
    }

    @PutMapping("/{id}")
    @AdminOnly
    @Operation(summary = "Update a course (every field is required)")
    public ApiResponse<CourseResponse> update(@PathVariable @Positive Long id,
                                              @Valid @RequestBody CourseRequest request) {
        return ApiResponse.success(courseService.update(id, request), "Course updated");
    }

    @DeleteMapping("/{id}")
    @AdminOnly
    @Operation(summary = "Soft-delete a course")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
