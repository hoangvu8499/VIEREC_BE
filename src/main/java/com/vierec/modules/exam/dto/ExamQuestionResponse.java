package com.vierec.modules.exam.dto;

import com.vierec.modules.exam.entity.ExamOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ExamQuestionResponse")
public class ExamQuestionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "1")
    private Long id;

    private String content;

    private String optionA;

    private String optionB;

    private String optionC;

    private String optionD;

    @Schema(example = "B")
    private ExamOption correctOption;

    @Schema(description = "Position in the exam; gaps are possible after a delete", example = "1")
    private Integer sortOrder;
}
