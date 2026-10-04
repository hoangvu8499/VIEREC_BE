package com.vierec.modules.exam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/** Exam of a course as seen by one of its learners: its rules and their attempt, never the answers. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MyExamResponse")
public class MyExamResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "1")
    private Long courseId;

    private String title;

    @Schema(example = "30")
    private Integer durationMinutes;

    @Schema(description = "Out of 10", example = "5")
    private BigDecimal passScore;

    @Schema(example = "20")
    private Integer questionCount;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(description = "null until the learner starts the exam")
    private ExamAttemptResponse attempt;
}
