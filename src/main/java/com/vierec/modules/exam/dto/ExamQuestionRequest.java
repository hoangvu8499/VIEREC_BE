package com.vierec.modules.exam.dto;

import com.vierec.modules.exam.entity.ExamOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * A question with its four options and the correct one. Body of the question create / update endpoints, and
 * also one row of an imported Excel file (validated with the same rules).
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "ExamQuestionRequest")
public class ExamQuestionRequest {

    public static final int CONTENT_MAX = 5000;
    public static final int OPTION_MAX = 1000;

    @NotBlank(message = "{examQuestion.content.required}")
    @Size(max = CONTENT_MAX, message = "{examQuestion.content.size}")
    @Schema(example = "Khi phát hiện rò rỉ khí gas, việc đầu tiên cần làm là gì?")
    private String content;

    @NotBlank(message = "{examQuestion.option.required}")
    @Size(max = OPTION_MAX, message = "{examQuestion.option.size}")
    @Schema(example = "Bật đèn để kiểm tra")
    private String optionA;

    @NotBlank(message = "{examQuestion.option.required}")
    @Size(max = OPTION_MAX, message = "{examQuestion.option.size}")
    @Schema(example = "Khoá van gas và mở cửa thông gió")
    private String optionB;

    @NotBlank(message = "{examQuestion.option.required}")
    @Size(max = OPTION_MAX, message = "{examQuestion.option.size}")
    @Schema(example = "Gọi điện thoại ngay trong phòng")
    private String optionC;

    @NotBlank(message = "{examQuestion.option.required}")
    @Size(max = OPTION_MAX, message = "{examQuestion.option.size}")
    @Schema(example = "Tiếp tục nấu ăn")
    private String optionD;

    @NotNull(message = "{examQuestion.correctOption.required}")
    @Schema(example = "B")
    private ExamOption correctOption;
}
