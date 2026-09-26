package com.vierec.modules.course.service.impl;

import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.common.exception.ResourceNotFoundException;
import com.vierec.modules.course.dto.CreateLessonRequest;
import com.vierec.modules.course.dto.LessonResponse;
import com.vierec.modules.course.dto.UpdateLessonRequest;
import com.vierec.modules.course.entity.Course;
import com.vierec.modules.course.entity.Lesson;
import com.vierec.modules.course.entity.LessonFile;
import com.vierec.modules.course.entity.LessonFileType;
import com.vierec.modules.course.mapper.CourseMapper;
import com.vierec.modules.course.repository.CourseRepository;
import com.vierec.modules.course.repository.LessonRepository;
import com.vierec.modules.course.service.LessonService;
import com.vierec.modules.file.entity.StoredFile;
import com.vierec.modules.file.service.FileCategory;
import com.vierec.modules.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonServiceImpl implements LessonService {

    /** Sub-folder of {@code app.upload.dir} for lesson files. */
    private static final String UPLOAD_FOLDER = "lessons";
    private static final int DOCUMENT_SORT = 1;
    private static final int VIDEO_SORT = 2;

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final FileStorageService fileStorageService;
    private final CourseMapper courseMapper;

    @Override
    @Transactional
    public LessonResponse create(Long courseId, CreateLessonRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND));
        if (lessonRepository.existsByCourseIdAndSortOrder(courseId, request.getSortOrder())) {
            throw new BusinessException(ErrorCode.LESSON_SORT_ORDER_EXISTS);
        }
        boolean withVideo = hasContent(request.getVideoFile());
        // Check both files before writing either, so a bad video does not leave a stored document behind.
        fileStorageService.validate(request.getDocumentFile(), FileCategory.DOCUMENT);
        if (withVideo) {
            fileStorageService.validate(request.getVideoFile(), FileCategory.VIDEO);
        }

        // Written files are removed again if anything below rolls the transaction back.
        StoredFile document = fileStorageService.store(request.getDocumentFile(), FileCategory.DOCUMENT,
                UPLOAD_FOLDER);

        Lesson lesson = new Lesson();
        lesson.setCourse(course);
        lesson.setTitle(request.getTitle().trim());
        lesson.setInstructions(request.getInstructions().trim());
        lesson.setSortOrder(request.getSortOrder());
        lesson.setDocumentUrl(FileStorageService.downloadUrl(document.getId()));
        lesson.setVideoUrl(normalizeUrl(request.getVideoUrl()));
        lesson.getLessonFiles().add(new LessonFile(lesson, document, LessonFileType.DOCUMENT, DOCUMENT_SORT));
        if (withVideo) {
            StoredFile video = fileStorageService.store(request.getVideoFile(), FileCategory.VIDEO, UPLOAD_FOLDER);
            lesson.getLessonFiles().add(new LessonFile(lesson, video, LessonFileType.VIDEO, VIDEO_SORT));
        }

        Lesson saved = lessonRepository.save(lesson);
        log.info("Created lesson id={} in course id={} (document id={}, video file={})",
                saved.getId(), courseId, document.getId(), withVideo);
        return courseMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public LessonResponse update(Long courseId, Long lessonId, UpdateLessonRequest request) {
        Lesson lesson = findEntity(courseId, lessonId);
        if (lessonRepository.existsByCourseIdAndSortOrderAndIdNot(courseId, request.getSortOrder(), lessonId)) {
            throw new BusinessException(ErrorCode.LESSON_SORT_ORDER_EXISTS);
        }
        boolean newDocument = hasContent(request.getDocumentFile());
        boolean newVideo = hasContent(request.getVideoFile());
        if (newDocument) {
            fileStorageService.validate(request.getDocumentFile(), FileCategory.DOCUMENT);
        }
        if (newVideo) {
            fileStorageService.validate(request.getVideoFile(), FileCategory.VIDEO);
        }

        lesson.setTitle(request.getTitle().trim());
        lesson.setInstructions(request.getInstructions().trim());
        lesson.setSortOrder(request.getSortOrder());
        lesson.setVideoUrl(normalizeUrl(request.getVideoUrl()));
        if (newDocument) {
            StoredFile document = fileStorageService.store(request.getDocumentFile(), FileCategory.DOCUMENT,
                    UPLOAD_FOLDER);
            replaceFile(lesson, document, LessonFileType.DOCUMENT, DOCUMENT_SORT);
            lesson.setDocumentUrl(FileStorageService.downloadUrl(document.getId()));
        }
        if (newVideo) {
            StoredFile video = fileStorageService.store(request.getVideoFile(), FileCategory.VIDEO, UPLOAD_FOLDER);
            replaceFile(lesson, video, LessonFileType.VIDEO, VIDEO_SORT);
        } else if (request.isRemoveVideo()) {
            // Unlinks only; the file itself is kept like every replaced file.
            lesson.getLessonFiles().removeIf(link -> link.getFileType() == LessonFileType.VIDEO);
        }

        Lesson saved = lessonRepository.saveAndFlush(lesson);
        log.info("Updated lesson id={} in course id={} (new document={}, new video={})",
                lessonId, courseId, newDocument, newVideo);
        return courseMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long courseId, Long lessonId) {
        Lesson lesson = findEntity(courseId, lessonId);
        lesson.setDeletedAt(LocalDateTime.now());
        lessonRepository.save(lesson);
        log.info("Soft-deleted lesson id={} in course id={}", lessonId, courseId);
    }

    /** The course is checked first so that lessons of a soft-deleted course are not found either. */
    private Lesson findEntity(Long courseId, Long lessonId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND);
        }
        return lessonRepository.findByIdAndCourseId(lessonId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.LESSON_NOT_FOUND));
    }

    /** An empty file input is still sent as a part of size 0; treat it as "keep the current file". */
    private static boolean hasContent(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private static String normalizeUrl(String url) {
        return StringUtils.hasText(url) ? url.trim() : null;
    }

    private static void replaceFile(Lesson lesson, StoredFile file, LessonFileType type, int sortOrder) {
        lesson.getLessonFiles().removeIf(link -> link.getFileType() == type);
        lesson.getLessonFiles().add(new LessonFile(lesson, file, type, sortOrder));
        // @OrderBy only applies when loading; keep the response in the same order as a later read.
        lesson.getLessonFiles().sort(Comparator.comparing(LessonFile::getSortOrder));
    }
}
