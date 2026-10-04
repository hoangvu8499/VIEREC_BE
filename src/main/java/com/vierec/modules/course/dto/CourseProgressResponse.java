package com.vierec.modules.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/** Video watching of a learner in a course, computed like the learner's page does. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CourseProgressResponse")
public class CourseProgressResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Tracked videos of the course (YouTube links and uploaded videos)", example = "4")
    private int videoCount;

    @Schema(description = "Videos never opened: their length is unknown", example = "1")
    private int unopenedCount;

    @Schema(description = "Sum of the known lengths", example = "1800")
    private long totalSeconds;

    @Schema(example = "1500")
    private long watchedSeconds;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(description = "watched / total x 100, rounded down; null when the course has no tracked video",
            example = "83")
    private Integer percent;

    @Schema(description = "Every video opened and at least 80% of the total watched (or no video at all)")
    private boolean examReady;

    private List<VideoProgressResponse> videos;
}
