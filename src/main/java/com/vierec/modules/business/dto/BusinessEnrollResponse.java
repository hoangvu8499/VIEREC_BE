package com.vierec.modules.business.dto;

import com.vierec.modules.course.dto.EnrollmentResponse;
import com.vierec.modules.course.entity.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "BusinessEnrollResponse")
public class BusinessEnrollResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "1")
    private Long courseId;

    private String courseName;

    @Schema(description = "PENDING enrollments created now, to be approved by an admin after the transfer")
    private List<EnrollmentResponse> enrolled;

    @Schema(description = "Learners already waiting for, learning or having completed the course")
    private List<Skipped> skipped;

    @Schema(description = "Amount to transfer for the new enrollments (VND)", example = "3000000")
    private long totalAmount;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "BusinessEnrollSkipped")
    public static class Skipped implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long userId;

        private String fullName;

        @Schema(description = "Current status of their enrollment")
        private EnrollmentStatus status;
    }
}
