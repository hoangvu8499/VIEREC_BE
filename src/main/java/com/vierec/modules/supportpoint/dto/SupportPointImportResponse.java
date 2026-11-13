package com.vierec.modules.supportpoint.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Result of a support point import: every row was saved, or none was and the call failed instead. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SupportPointImportResponse")
public class SupportPointImportResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Support points created by this import", example = "40")
    private Integer importedCount;
}
