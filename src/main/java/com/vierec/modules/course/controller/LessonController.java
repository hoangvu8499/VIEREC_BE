package com.vierec.modules.course.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.common.dto.ApiResponse;
import com.vierec.modules.course.dto.CreateLessonRequest;
import com.vierec.modules.course.dto.LessonResponse;
import com.vierec.modules.course.dto.UpdateLessonRequest;
import com.vierec.modules.course.service.LessonService;
import com.vierec.security.access.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Positive;

@RestController
@RequestMapping(AppConstants.API_V1 + "/courses/{courseId}/lessons")
@RequiredArgsConstructor
@Validated
@Tag(name = "Lessons", description = "Lessons of a course")
@SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
public class LessonController {

    private final LessonService lessonService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AdminOnly
    @Operation(summary = "Create a lesson (multipart/form-data)",
            description = "title, instructions, sortOrder and documentFile are required. videoUrl is an optional "
                    + "YouTube link.")
    public ResponseEntity<ApiResponse<LessonResponse>> create(@PathVariable @Positive Long courseId,
                                                              @Valid @ModelAttribute CreateLessonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(lessonService.create(courseId, request), "Lesson created"));
    }

    @PutMapping(value = "/{lessonId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AdminOnly
    @Operation(summary = "Update a lesson (multipart/form-data)",
            description = "title, instructions and sortOrder are required. documentFile is optional: send it to "
                    + "replace the document, leave it out to keep the current one. videoUrl is a YouTube link.")
    public ApiResponse<LessonResponse> update(@PathVariable @Positive Long courseId,
                                              @PathVariable @Positive Long lessonId,
                                              @Valid @ModelAttribute UpdateLessonRequest request) {
        return ApiResponse.success(lessonService.update(courseId, lessonId, request), "Lesson updated");
    }

    @DeleteMapping("/{lessonId}")
    @AdminOnly
    @Operation(summary = "Soft-delete a lesson")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long courseId,
                                       @PathVariable @Positive Long lessonId) {
        lessonService.delete(courseId, lessonId);
        return ResponseEntity.noContent().build();
    }
}
