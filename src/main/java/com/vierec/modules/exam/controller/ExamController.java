package com.vierec.modules.exam.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.common.dto.ApiResponse;
import com.vierec.modules.exam.dto.ExamImportRequest;
import com.vierec.modules.exam.dto.ExamImportResponse;
import com.vierec.modules.exam.dto.ExamQuestionRequest;
import com.vierec.modules.exam.dto.ExamQuestionResponse;
import com.vierec.modules.exam.dto.ExamRequest;
import com.vierec.modules.exam.dto.ExamResponse;
import com.vierec.modules.exam.service.ExamQuestionWorkbook;
import com.vierec.modules.exam.service.ExamService;
import com.vierec.security.access.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Exams", description = "Certificate exam of a course: settings and multiple-choice questions")
@SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
@AdminOnly
public class ExamController {

    private static final String EXAM = AppConstants.API_V1 + "/courses/{courseId}/exam";
    private static final String QUESTIONS = EXAM + "/questions";
    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExamService examService;
    private final ExamQuestionWorkbook workbook;

    @GetMapping(EXAM)
    @Operation(summary = "Exam of a course with every question and correct answer",
            description = "404 EXAM_NOT_FOUND (VRC-404-701) while the course has no exam.")
    public ApiResponse<ExamResponse> get(@PathVariable @Positive Long courseId) {
        return ApiResponse.success(examService.get(courseId));
    }

    @PutMapping(EXAM)
    @Operation(summary = "Create the exam of a course, or change its settings")
    public ApiResponse<ExamResponse> save(@PathVariable @Positive Long courseId,
                                          @Valid @RequestBody ExamRequest request) {
        return ApiResponse.success(examService.save(courseId, request), "Exam saved");
    }

    @PostMapping(QUESTIONS)
    @Operation(summary = "Add a question after the last one")
    public ResponseEntity<ApiResponse<ExamQuestionResponse>> addQuestion(
            @PathVariable @Positive Long courseId, @Valid @RequestBody ExamQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(examService.addQuestion(courseId, request), "Question added"));
    }

    @PutMapping(QUESTIONS + "/{questionId}")
    @Operation(summary = "Replace a question, its options and its correct answer")
    public ApiResponse<ExamQuestionResponse> updateQuestion(@PathVariable @Positive Long courseId,
                                                            @PathVariable @Positive Long questionId,
                                                            @Valid @RequestBody ExamQuestionRequest request) {
        return ApiResponse.success(examService.updateQuestion(courseId, questionId, request), "Question updated");
    }

    @DeleteMapping(QUESTIONS + "/{questionId}")
    @Operation(summary = "Delete a question")
    public ResponseEntity<Void> deleteQuestion(@PathVariable @Positive Long courseId,
                                               @PathVariable @Positive Long questionId) {
        examService.deleteQuestion(courseId, questionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = QUESTIONS + "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import questions from an Excel file (multipart/form-data)",
            description = "mode APPEND adds the file's questions after the current ones, REPLACE deletes the current "
                    + "ones first. Nothing is saved when a row is invalid: 400 EXAM_IMPORT_INVALID_ROWS lists every "
                    + "bad cell in errors as rows[<Excel row>].<field>.")
    public ApiResponse<ExamImportResponse> importQuestions(@PathVariable @Positive Long courseId,
                                                           @Valid @ModelAttribute ExamImportRequest request) {
        ExamImportResponse result = examService.importQuestions(courseId, request.getFile(), request.getMode());
        return ApiResponse.success(result, "Imported " + result.getImportedCount() + " questions");
    }

    @GetMapping(AppConstants.API_V1 + "/exams/question-template")
    @Operation(summary = "Download the Excel template for the question import")
    public ResponseEntity<byte[]> questionTemplate() {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("mau-cau-hoi-bai-thi.xlsx", StandardCharsets.UTF_8).build().toString())
                .body(workbook.template());
    }
}
