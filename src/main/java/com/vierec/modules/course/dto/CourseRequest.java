package com.vierec.modules.course.dto;

import com.vierec.modules.course.entity.CourseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

/** Body of {@code POST /api/v1/courses} and {@code PUT /api/v1/courses/{id}}. Every field is required. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CourseRequest")
public class CourseRequest {

    /** {@code courses.description} is TEXT (65,535 bytes); 10,000 characters fit even in 4-byte UTF-8. */
    public static final int DESCRIPTION_MAX = 10000;

    /** Smallest amount Vietnamese banks accept for a transfer. */
    public static final long PRICE_MIN = 1000L;
    public static final long PRICE_MAX = 1_000_000_000L;

    @NotBlank(message = "{course.name.required}")
    @Size(max = 200, message = "{course.name.size}")
    @Schema(example = "Phòng cháy chữa cháy cơ bản")
    private String name;

    @NotBlank(message = "{course.description.required}")
    @Size(max = DESCRIPTION_MAX, message = "{course.description.size}")
    @Schema(example = "Kiến thức cơ bản về phòng cháy chữa cháy cho nhân viên mới")
    private String description;

    @NotNull(message = "{course.instructorId.required}")
    @Positive(message = "{course.instructorId.positive}")
    @Schema(description = "Id of an active user who teaches the course", example = "2")
    private Long instructorId;

    @NotNull(message = "{course.price.required}")
    @Min(value = PRICE_MIN, message = "{course.price.range}")
    @Max(value = PRICE_MAX, message = "{course.price.range}")
    @Schema(description = "Học phí (VND)", example = "100000")
    private Long price;

    @NotNull(message = "{course.status.required}")
    @Schema(example = "DRAFT")
    private CourseStatus status;
}
