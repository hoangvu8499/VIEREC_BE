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

/** Exam with every question and its correct answer (admin only). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ExamResponse")
public class ExamResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "1")
    private Long id;

    @Schema(example = "1")
    private Long courseId;

    private String title;

    @Schema(example = "30")
    private Integer durationMinutes;

    @Schema(description = "Minimum score to pass, out of 10", example = "5")
    private BigDecimal passScore;

    @Schema(example = "20")
    private Integer questionCount;

    @Schema(description = "In exam order")
    private List<ExamQuestionResponse> questions;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
