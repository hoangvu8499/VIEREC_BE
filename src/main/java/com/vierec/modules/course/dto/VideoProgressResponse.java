package com.vierec.modules.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "VideoProgressResponse")
public class VideoProgressResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "11")
    private Long lessonId;

    @Schema(example = "file-2")
    private String videoKey;

    @Schema(example = "600")
    private int durationSeconds;

    @Schema(example = "480")
    private int watchedSeconds;
}
