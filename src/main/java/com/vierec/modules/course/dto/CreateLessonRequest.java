package com.vierec.modules.course.dto;

import com.vierec.common.validation.RequiredFile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Multipart form of {@code POST /api/v1/courses/{courseId}/lessons}. Title, instructions, sort order and the
 * document are required; the document goes to {@code lessons.document_url} and {@code lesson_files}.
 * The video is optional: an uploaded file ({@code lesson_files}), a link to another system
 * ({@code lessons.video_url}), both or neither.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "CreateLessonRequest")
public class CreateLessonRequest {

    /** Length of {@code lessons.video_url}. */
    public static final int VIDEO_URL_MAX = 2048;
    /** Absolute http(s) link without whitespace; blank is allowed and means "no link". */
    public static final String VIDEO_URL_PATTERN = "^\\s*((?i)https?://\\S+)?\\s*$";

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

    @RequiredFile(message = "{lesson.documentFile.required}")
    @Schema(description = "Document: pdf, doc, docx, ppt, pptx, xls, xlsx, txt", type = "string", format = "binary")
    private MultipartFile documentFile;

    @Schema(description = "Optional video: mp4, webm, mov, mkv", type = "string", format = "binary")
    private MultipartFile videoFile;

    @Size(max = VIDEO_URL_MAX, message = "{lesson.videoUrl.size}")
    @Pattern(regexp = VIDEO_URL_PATTERN, message = "{lesson.videoUrl.pattern}")
    @Schema(description = "Optional link to a video on another system", example = "https://youtu.be/abc123")
    private String videoUrl;
}
