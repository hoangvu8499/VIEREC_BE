package com.vierec.modules.course.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.common.dto.ApiResponse;
import com.vierec.common.dto.PageResponse;
import com.vierec.modules.course.dto.CourseDetailResponse;
import com.vierec.modules.course.dto.CourseResponse;
import com.vierec.modules.course.dto.CourseRequest;
import com.vierec.modules.course.entity.CourseStatus;
import com.vierec.modules.course.service.CourseService;
import com.vierec.modules.role.entity.RoleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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

    private static final String ADMIN_AUTHORITY = "ROLE_" + RoleCode.ADMIN;
    private static final String SUPER_ADMIN_AUTHORITY = "ROLE_" + RoleCode.SUPER_ADMIN;

    private final CourseService courseService;

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "List courses, 10 per page, newest first (no login required)",
            description = "SUPER_ADMIN / ADMIN see every status and may filter by status; "
                    + "anonymous and other users only see PUBLISHED courses.")
    public ApiResponse<PageResponse<CourseResponse>> list(
            @Parameter(description = "0-based page number") @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{page.min}") int page,
            @Parameter(description = "Part of the course name") @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CourseStatus status,
            @Parameter(hidden = true) Authentication authentication) {
        return ApiResponse.success(courseService.list(keyword, status, page, !isAdmin(authentication)));
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Course detail with its lessons (no login required)",
            description = "Anonymous and non-admin users get 404 for courses that are not PUBLISHED.")
    public ApiResponse<CourseDetailResponse> get(@PathVariable @Positive Long id,
                                                 @Parameter(hidden = true) Authentication authentication) {
        return ApiResponse.success(courseService.get(id, !isAdmin(authentication)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create a course (every field is required)")
    public ResponseEntity<ApiResponse<CourseResponse>> create(@Valid @RequestBody CourseRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(courseService.create(request, authentication.getName()),
                        "Course created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update a course (every field is required)")
    public ApiResponse<CourseResponse> update(@PathVariable @Positive Long id,
                                              @Valid @RequestBody CourseRequest request) {
        return ApiResponse.success(courseService.update(id, request), "Course updated");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Soft-delete a course")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** {@code authentication} is null for anonymous callers of the public list. */
    private static boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String name = authority.getAuthority();
            if (ADMIN_AUTHORITY.equals(name) || SUPER_ADMIN_AUTHORITY.equals(name)) {
                return true;
            }
        }
        return false;
    }
}
