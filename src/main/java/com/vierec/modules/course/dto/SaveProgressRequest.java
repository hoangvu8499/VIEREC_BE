package com.vierec.modules.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(name = "SaveProgressRequest")
public class SaveProgressRequest {

    public static final int VIDEOS_MAX = 500;

    @Schema(description = "Videos of the course; values only grow: a smaller watchedSeconds than the stored one is "
            + "ignored. Videos that are not part of the course are skipped.")
    @NotNull(message = "{progress.videos.required}")
    @Size(max = VIDEOS_MAX, message = "{progress.videos.size}")
    private List<@Valid VideoProgressRequest> videos;
}
