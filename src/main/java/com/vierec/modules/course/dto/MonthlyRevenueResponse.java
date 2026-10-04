package com.vierec.modules.course.dto;

import com.vierec.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/** Revenue of the enrollments approved in one month: totals, per course, and who paid what. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MonthlyRevenueResponse")
public class MonthlyRevenueResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "First day of the month", example = "2026-10-01")
    private LocalDate from;

    @Schema(description = "Last day of the month", example = "2026-10-31")
    private LocalDate to;

    @Schema(description = "Sum of the enrollment prices of the whole month (VND)", example = "1500000")
    private long amount;

    @Schema(description = "Approved enrollments of the whole month", example = "15")
    private long enrollments;

    @Schema(description = "Revenue per course, highest first (whole month, keyword ignored)")
    private List<CourseRevenue> courses;

    @Schema(description = "Sum of the enrollments matching the keyword; equals amount without keyword",
            example = "300000")
    private long matchedAmount;

    @Schema(description = "Approved enrollments matching the keyword, latest approval first")
    private PageResponse<EnrollmentResponse> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "CourseRevenue")
    public static class CourseRevenue implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(example = "7")
        private Long courseId;

        @Schema(example = "Phòng cháy chữa cháy cơ bản")
        private String courseName;

        @Schema(example = "1000000")
        private long amount;

        @Schema(example = "10")
        private long enrollments;
    }
}
