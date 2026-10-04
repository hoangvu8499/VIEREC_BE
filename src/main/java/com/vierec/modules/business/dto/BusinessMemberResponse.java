package com.vierec.modules.business.dto;

import com.vierec.modules.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** A learner of a business with a summary of their courses. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "BusinessMemberResponse")
public class BusinessMemberResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "12")
    private Long id;

    @Schema(example = "nv.an")
    private String username;

    private String firstName;

    private String lastName;

    @Schema(description = "Last name + first name", example = "Nguyễn Văn An")
    private String fullName;

    private String cccd;

    private LocalDate dateOfBirth;

    private String phoneNumber;

    private String email;

    private UserStatus status;

    private LocalDateTime createdAt;

    @Schema(description = "Enrollments waiting for approval", example = "1")
    private long pendingCount;

    @Schema(description = "Courses being learned (ENROLLED)", example = "2")
    private long learningCount;

    @Schema(description = "Courses completed", example = "1")
    private long completedCount;

    @Schema(example = "1")
    private long certificateCount;
}
