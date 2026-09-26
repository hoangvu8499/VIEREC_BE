package com.vierec.modules.course.dto;

import com.vierec.modules.course.entity.CourseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/** One course, flat so that it maps straight onto a table row. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CourseResponse", description = "Course as one flat table row")
public class CourseResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "1")
    private Long id;

    @Schema(example = "Phòng cháy chữa cháy cơ bản")
    private String name;

    private String description;

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

    @Schema(example = "3")
    private long lessonCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
