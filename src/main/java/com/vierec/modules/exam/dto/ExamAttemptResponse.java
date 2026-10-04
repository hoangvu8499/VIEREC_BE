package com.vierec.modules.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** The logged-in learner's attempt at an exam: the questions while answering, the result once graded. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ExamAttemptResponse")
public class ExamAttemptResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "1")
    private Long id;

    private ExamAttemptStatus status;

    private LocalDateTime startedAt;

    private LocalDateTime deadlineAt;

    @Schema(description = "Seconds left, counted by the server (IN_PROGRESS only): count down from this value "
            + "instead of comparing deadlineAt with the device clock", example = "1795")
    private Long remainingSeconds;

    @Schema(description = "Questions without their answer, in exam order (IN_PROGRESS only)")
    private List<ExamPaperQuestionResponse> questions;

    @Schema(description = "Pass score at start time, out of 10", example = "5")
    private BigDecimal passScore;

    @Schema(description = "SUBMITTED only")
    private LocalDateTime submittedAt;

    @Schema(description = "Number of questions when graded (SUBMITTED only)", example = "20")
    private Integer questionCount;

    @Schema(description = "SUBMITTED only", example = "17")
    private Integer correctCount;

    @Schema(description = "Out of 10, 2 decimals (SUBMITTED only)", example = "8.5")
    private BigDecimal score;

    @Schema(description = "SUBMITTED only")
    private Boolean passed;
}
