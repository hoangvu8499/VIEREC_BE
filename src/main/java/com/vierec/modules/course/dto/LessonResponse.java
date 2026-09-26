package com.vierec.modules.course.dto;

import com.vierec.modules.course.entity.LessonFileType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LessonResponse")
public class LessonResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "1")
    private Long id;

    @Schema(example = "1")
    private Long courseId;

    private String title;

    private String instructions;

    @Schema(example = "1")
    private Integer sortOrder;

    @Schema(description = "Download link of the lesson document", example = "/api/v1/files/1")
    private String documentUrl;

    @Schema(description = "Link to a video on another system, if any", example = "https://youtu.be/abc123")
    private String videoUrl;

    private List<FileItem> files;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "LessonFileResponse")
    public static class FileItem implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(example = "1")
        private Long fileId;

        @Schema(example = "DOCUMENT")
        private LessonFileType fileType;

        @Schema(example = "bai-1.pdf")
        private String originalName;

        @Schema(example = "application/pdf")
        private String contentType;

        @Schema(example = "204800")
        private Long sizeBytes;

        @Schema(description = "GET this (with the access_token cookie) to download or stream the file",
                example = "/api/v1/files/1")
        private String url;

        @Schema(example = "1")
        private Integer sortOrder;
    }
}
