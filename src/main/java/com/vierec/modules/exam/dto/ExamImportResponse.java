package com.vierec.modules.exam.dto;

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
@Schema(name = "ExamImportResponse")
public class ExamImportResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Questions read from the file", example = "20")
    private Integer importedCount;

    @Schema(description = "The exam after the import")
    private ExamResponse exam;
}
