package com.vierec.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/** {@code POST /businesses}: the business and its first manager account, created together. */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "CreateBusinessRequest")
public class CreateBusinessRequest extends BusinessRequest {

    @Valid
    @NotNull(message = "{business.manager.required}")
    @Schema(description = "First manager account (role BUSINESS)")
    private BusinessManagerRequest manager;
}
