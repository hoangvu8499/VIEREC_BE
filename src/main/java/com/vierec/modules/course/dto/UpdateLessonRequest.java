package com.vierec.modules.course.dto;

import com.vierec.modules.course.validation.YoutubeUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Multipart form of {@code PUT /api/v1/courses/{courseId}/lessons/{lessonId}}. Title, instructions and sort
 * order are required. A new document replaces the current one and may be left out (or empty) to keep it.
 * {@code videoUrl} (YouTube) is replaced like the text fields: leaving it out removes the link.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "UpdateLessonRequest")
public class UpdateLessonRequest {

    @NotBlank(message = "{lesson.title.required}")
    @Size(max = 200, message = "{lesson.title.size}")
    @Schema(example = "Bài 1: Nhận biết nguy cơ cháy nổ")
    private String title;

    @NotBlank(message = "{lesson.instructions.required}")
    @Size(max = CourseRequest.DESCRIPTION_MAX, message = "{lesson.instructions.size}")
    @Schema(example = "Đọc tài liệu, sau đó xem hết video.")
    private String instructions;

    @NotNull(message = "{lesson.sortOrder.required}")
    @Min(value = 1, message = "{lesson.sortOrder.min}")
    @Max(value = 10000, message = "{lesson.sortOrder.max}")
    @Schema(description = "Position of the lesson in the course, unique per course", example = "1")
    private Integer sortOrder;

    @Schema(description = "Optional new document: pdf, doc, docx, ppt, pptx, xls, xlsx, txt",
            type = "string", format = "binary")
    private MultipartFile documentFile;

    @Schema(description = "true unlinks the video file uploaded before lessons became YouTube only")
    private boolean removeVideo;

    @Size(max = CreateLessonRequest.VIDEO_URL_MAX, message = "{lesson.videoUrl.size}")
    @YoutubeUrl
    @Schema(description = "YouTube link of the lecture video; empty or missing removes the link",
            example = "https://youtu.be/dQw4w9WgXcQ")
    private String videoUrl;
}
