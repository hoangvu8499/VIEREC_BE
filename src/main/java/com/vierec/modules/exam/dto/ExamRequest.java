package com.vierec.modules.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/** Body of {@code PUT /api/v1/courses/{courseId}/exam}: creates the exam or replaces its settings. */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "ExamRequest")
public class ExamRequest {

    @NotBlank(message = "{exam.title.required}")
    @Size(max = 200, message = "{exam.title.size}")
    @Schema(example = "Bài thi chứng chỉ An toàn hoá chất")
    private String title;

    @NotNull(message = "{exam.durationMinutes.required}")
    @Min(value = 1, message = "{exam.durationMinutes.range}")
    @Max(value = 300, message = "{exam.durationMinutes.range}")
    @Schema(description = "Time limit in minutes", example = "30")
    private Integer durationMinutes;

    @NotNull(message = "{exam.passScore.required}")
    @DecimalMin(value = "0", inclusive = false, message = "{exam.passScore.range}")
    @DecimalMax(value = "10", message = "{exam.passScore.range}")
    @Digits(integer = 2, fraction = 2, message = "{exam.passScore.digits}")
    @Schema(description = "Minimum score to pass, out of 10", example = "5")
    private BigDecimal passScore;
}
