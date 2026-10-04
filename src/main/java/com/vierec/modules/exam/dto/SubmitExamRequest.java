package com.vierec.modules.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(name = "SubmitExamRequest")
public class SubmitExamRequest {

    /** Only guards against oversized bodies: exams are far shorter. */
    public static final int ANSWERS_MAX = 1000;

    @Schema(description = "Answered questions only; unknown question ids are ignored, the last answer of a question "
            + "counts")
    @NotNull(message = "{examSubmit.answers.required}")
    @Size(max = ANSWERS_MAX, message = "{examSubmit.answers.size}")
    private List<@Valid ExamAnswerRequest> answers;
}
