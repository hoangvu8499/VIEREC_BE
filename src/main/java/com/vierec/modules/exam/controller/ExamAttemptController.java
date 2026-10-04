package com.vierec.modules.exam.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.common.dto.ApiResponse;
import com.vierec.modules.exam.dto.MyExamResponse;
import com.vierec.modules.exam.dto.SubmitExamRequest;
import com.vierec.modules.exam.service.ExamAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Positive;

/**
 * The logged-in learner taking the certificate exam of a course. Every endpoint answers 403 EXAM_NOT_ALLOWED
 * (VRC-403-701) unless the learner's enrollment is ENROLLED or COMPLETED, and 404 EXAM_NOT_FOUND when the course has
 * no exam.
 */
@RestController
@RequestMapping(AppConstants.API_V1 + "/courses/{courseId}/my-exam")
@RequiredArgsConstructor
@Validated
@Tag(name = "My exam", description = "Taking the certificate exam of a course (learner side)")
@SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
public class ExamAttemptController {

    private final ExamAttemptService attemptService;

    @GetMapping
    @Operation(summary = "Exam rules and my attempt",
            description = "attempt is null before the start. IN_PROGRESS carries the questions (without answers) "
                    + "and remainingSeconds; SUBMITTED carries the result. An attempt whose time ran out without a "
                    + "submit is graded with no answers.")
    public ApiResponse<MyExamResponse> get(@PathVariable @Positive Long courseId,
                                           @Parameter(hidden = true) Authentication authentication) {
        return ApiResponse.success(attemptService.get(courseId, authentication.getName()));
    }

    @PostMapping("/start")
    @Operation(summary = "Start the exam (only once); the timer runs on the server from now",
            description = "Returns the current attempt unchanged when it was already started. "
                    + "409 EXAM_HAS_NO_QUESTIONS (VRC-409-701) while the exam has no question.")
    public ApiResponse<MyExamResponse> start(@PathVariable @Positive Long courseId,
                                             @Parameter(hidden = true) Authentication authentication) {
        return ApiResponse.success(attemptService.start(courseId, authentication.getName()), "Exam started");
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit my answers and get the score",
            description = "Answers arriving more than 1 minute after the deadline are not counted. Passing sets the "
                    + "enrollment to COMPLETED. 404 EXAM_ATTEMPT_NOT_FOUND (VRC-404-703) before the start, "
                    + "409 EXAM_ALREADY_TAKEN (VRC-409-702) once submitted.")
    public ApiResponse<MyExamResponse> submit(@PathVariable @Positive Long courseId,
                                              @Valid @RequestBody SubmitExamRequest request,
                                              @Parameter(hidden = true) Authentication authentication) {
        return ApiResponse.success(attemptService.submit(courseId, authentication.getName(), request.getAnswers()),
                "Exam submitted");
    }
}
