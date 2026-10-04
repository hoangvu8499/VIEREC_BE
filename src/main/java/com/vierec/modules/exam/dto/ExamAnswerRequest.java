package com.vierec.modules.exam.dto;

import com.vierec.modules.exam.entity.ExamOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ExamAnswerRequest")
public class ExamAnswerRequest {

    @NotNull(message = "{examAnswer.questionId.required}")
    @Schema(example = "1")
    private Long questionId;

    @NotNull(message = "{examAnswer.selectedOption.required}")
    @Schema(example = "B")
    private ExamOption selectedOption;
}
