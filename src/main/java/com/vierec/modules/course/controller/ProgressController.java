package com.vierec.modules.course.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.common.dto.ApiResponse;
import com.vierec.modules.course.dto.CourseProgressResponse;
import com.vierec.modules.course.dto.SaveProgressRequest;
import com.vierec.modules.course.service.LearningProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Positive;

/** Video watching of the logged-in learner. 403 PROGRESS_NOT_ALLOWED unless their enrollment is approved. */
@RestController
@RequestMapping(AppConstants.API_V1 + "/courses/{courseId}/my-progress")
@RequiredArgsConstructor
@Validated
@Tag(name = "My progress", description = "Video watching of the logged-in learner in a course")
@SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
public class ProgressController {

    private final LearningProgressService progressService;

    @GetMapping
    @Operation(summary = "My video progress in the course, with the exam condition (80% of the video time)")
    public ApiResponse<CourseProgressResponse> get(@PathVariable @Positive Long courseId,
                                                   @Parameter(hidden = true) Authentication authentication) {
        return ApiResponse.success(progressService.getMine(courseId, authentication.getName()));
    }

    @PutMapping
    @Operation(summary = "Report watched videos; stored values only grow",
            description = "Videos that are not tracked videos of the course are skipped.")
    public ApiResponse<CourseProgressResponse> save(@PathVariable @Positive Long courseId,
                                                    @Valid @RequestBody SaveProgressRequest request,
                                                    @Parameter(hidden = true) Authentication authentication) {
        return ApiResponse.success(progressService.saveMine(courseId, authentication.getName(), request.getVideos()));
    }
}
