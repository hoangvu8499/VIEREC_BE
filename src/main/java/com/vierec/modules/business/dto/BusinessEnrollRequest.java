package com.vierec.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Set;

/** {@code POST /my-business/enrollments}: enroll learners of the business in one course. */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "BusinessEnrollRequest")
public class BusinessEnrollRequest {

    public static final int USERS_MAX = 200;

    @NotNull(message = "{businessEnroll.courseId.required}")
    @Schema(example = "1")
    private Long courseId;

    @NotEmpty(message = "{businessEnroll.userIds.required}")
    @Size(max = USERS_MAX, message = "{businessEnroll.userIds.size}")
    @Schema(description = "Learners of the business", example = "[12, 13]")
    private Set<@NotNull(message = "{businessEnroll.userIds.required}") Long> userIds;
}
