package com.vierec.modules.course.dto;

import com.vierec.modules.course.entity.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Schema(name = "UpdateEnrollmentRequest")
public class UpdateEnrollmentRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "{enrollment.status.required}")
    @Schema(example = "COMPLETED", requiredMode = Schema.RequiredMode.REQUIRED)
    private EnrollmentStatus status;
}
