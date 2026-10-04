package com.vierec.modules.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vierec.modules.course.entity.CourseStatus;
import com.vierec.modules.course.entity.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/** Course fields of {@link CourseResponse} plus its live lessons in course order. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CourseDetailResponse")
public class CourseDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "1")
    private Long id;

    @Schema(example = "Phòng cháy chữa cháy cơ bản")
    private String name;

    private String description;

    @Schema(description = "Học phí (VND)", example = "100000")
    private Long price;

    @Schema(example = "PUBLISHED")
    private CourseStatus status;

    @Schema(example = "2")
    private Long instructorId;

    @Schema(example = "vutth")
    private String instructorUsername;

    @Schema(description = "Last name + first name", example = "Trần Vũ")
    private String instructorName;

    @Schema(example = "vutth")
    private String createdByUsername;

    // Sent as null too (the app default drops nulls): the client branches on it.
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(description = "Enrollment status of the caller; null when not enrolled or not logged in",
            example = "ENROLLED")
    private EnrollmentStatus myEnrollmentStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Schema(description = "Sorted by sortOrder")
    private List<LessonResponse> lessons;
}
