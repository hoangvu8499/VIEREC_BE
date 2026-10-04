package com.vierec.modules.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "VideoProgressRequest")
public class VideoProgressRequest {

    /** A day: longer videos are not lessons. */
    public static final int SECONDS_MAX = 86_400;

    @NotNull(message = "{progress.lessonId.required}")
    @Schema(example = "11")
    private Long lessonId;

    @NotBlank(message = "{progress.videoKey.required}")
    @Size(max = 100, message = "{progress.videoKey.size}")
    @Schema(description = "youtube-<video id> or file-<file id>", example = "file-2")
    private String videoKey;

    @NotNull(message = "{progress.seconds.required}")
    @Min(value = 0, message = "{progress.seconds.range}")
    @Max(value = SECONDS_MAX, message = "{progress.seconds.range}")
    @Schema(description = "0 while the length is unknown", example = "600")
    private Integer durationSeconds;

    @NotNull(message = "{progress.seconds.required}")
    @Min(value = 0, message = "{progress.seconds.range}")
    @Max(value = SECONDS_MAX, message = "{progress.seconds.range}")
    @Schema(description = "Distinct seconds watched", example = "480")
    private Integer watchedSeconds;
}
