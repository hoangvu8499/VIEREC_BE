package com.vierec.modules.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Question as shown to a learner taking the exam: without its correct answer. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ExamPaperQuestionResponse")
public class ExamPaperQuestionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "1")
    private Long id;

    private String content;

    private String optionA;

    private String optionB;

    private String optionC;

    private String optionD;
}
