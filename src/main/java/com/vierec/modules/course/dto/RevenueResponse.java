package com.vierec.modules.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

/** Revenue of approved enrollments in the current week, month and year. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RevenueResponse")
public class RevenueResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "From Monday of this week")
    private Period week;

    private Period month;

    private Period year;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "RevenuePeriod")
    public static class Period implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "First day of the period, up to today", example = "2026-09-28")
        private LocalDate from;

        @Schema(description = "Sum of the enrollment prices (VND)", example = "1500000")
        private long amount;

        @Schema(description = "Approved enrollments counted", example = "15")
        private long enrollments;
    }
}
