package com.vierec.modules.course.service;

import com.vierec.modules.course.dto.CreateLessonRequest;
import com.vierec.modules.course.dto.LessonResponse;
import com.vierec.modules.course.dto.UpdateLessonRequest;

public interface LessonService {

    /** Stores the document (and the video, if sent) on the server, then creates the lesson with them attached. */
    LessonResponse create(Long courseId, CreateLessonRequest request);

    /**
     * Updates the text fields; a non-empty document / video part replaces that file. The replaced file stays on
     * disk and in {@code files}, only its {@code lesson_files} link is removed.
     */
    LessonResponse update(Long courseId, Long lessonId, UpdateLessonRequest request);

    /** Soft delete: sets {@code deleted_at}; the lesson's files are kept. */
    void delete(Long courseId, Long lessonId);
}
