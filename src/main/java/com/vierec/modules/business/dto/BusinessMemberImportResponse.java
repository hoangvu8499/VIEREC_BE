package com.vierec.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/** Result of a learner import: every account was created, or none was and the call failed instead. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "BusinessMemberImportResponse")
public class BusinessMemberImportResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Accounts created by this import", example = "120")
    private Integer importedCount;

    @Schema(description = "The accounts just created, in file order")
    private List<BusinessMemberResponse> members;
}
