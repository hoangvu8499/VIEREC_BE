package com.vierec.modules.exam.dto;

import com.vierec.common.validation.RequiredFile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

/** Multipart form of {@code POST /api/v1/courses/{courseId}/exam/questions/import}. */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "ExamImportRequest")
public class ExamImportRequest {

    @RequiredFile(message = "{examImport.file.required}")
    @Schema(description = "Excel file (.xlsx or .xls) laid out like GET /api/v1/exams/question-template",
            type = "string", format = "binary")
    private MultipartFile file;

    @NotNull(message = "{examImport.mode.required}")
    @Schema(example = "APPEND")
    private ExamImportMode mode;
}
