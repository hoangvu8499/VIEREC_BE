package com.vierec.modules.business.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vierec.modules.course.dto.CourseProgressResponse;
import com.vierec.modules.course.entity.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One course of a business learner: enrollment, video progress, exam result and certificate. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MemberCourseResponse")
public class MemberCourseResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "40")
    private Long enrollmentId;

    @Schema(example = "1")
    private Long courseId;

    @Schema(example = "Phòng cháy chữa cháy cơ bản")
    private String courseName;

    private EnrollmentStatus status;

    @Schema(description = "Price when the enrollment was requested (VND)", example = "1500000")
    private Long price;

    private LocalDateTime enrolledAt;

    private LocalDateTime approvedAt;

    private LocalDateTime completedAt;

    @Schema(description = "Video watching (without the per-video list)")
    private CourseProgressResponse progress;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(description = "null until the learner starts the exam")
    private Exam exam;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(description = "null until a certificate is issued")
    private Certificate certificate;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "MemberExamResponse")
    public static class Exam implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "false while the learner is answering")
        private boolean submitted;

        private LocalDateTime startedAt;

        private LocalDateTime submittedAt;

        private Integer questionCount;

        private Integer correctCount;

        @Schema(description = "Out of 10", example = "8.5")
        private BigDecimal score;

        private BigDecimal passScore;

        private Boolean passed;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "MemberCertificateResponse")
    public static class Certificate implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(example = "VRC-2026-AB12CD34")
        private String code;

        private LocalDateTime issuedAt;
    }
}
