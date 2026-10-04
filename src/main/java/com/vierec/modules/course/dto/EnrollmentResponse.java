package com.vierec.modules.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vierec.modules.course.entity.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/** One enrollment with the course and trainee fields a list row needs. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "EnrollmentResponse")
public class EnrollmentResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "15")
    private Long id;

    @Schema(example = "7")
    private Long courseId;

    @Schema(example = "Phòng cháy chữa cháy cơ bản")
    private String courseName;

    private String courseDescription;

    @Schema(description = "Last name + first name", example = "Nguyễn Quản Trị")
    private String instructorName;

    @Schema(example = "5")
    private long lessonCount;

    @Schema(example = "3")
    private Long userId;

    @Schema(example = "nguyenvana")
    private String username;

    @Schema(description = "Trainee: last name + first name", example = "Nguyễn Văn An")
    private String fullName;

    @Schema(description = "Business of the trainee; null for an independent learner",
            example = "Công ty TNHH Môi trường Xanh")
    private String businessName;

    @Schema(example = "ENROLLED")
    private EnrollmentStatus status;

    @Schema(description = "Amount to transfer (VND): the course price when the learner asked to join",
            example = "100000")
    private Long price;

    private LocalDateTime enrolledAt;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(description = "When an admin approved the enrollment; null while PENDING or CANCELLED")
    private LocalDateTime approvedAt;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private LocalDateTime completedAt;
}
