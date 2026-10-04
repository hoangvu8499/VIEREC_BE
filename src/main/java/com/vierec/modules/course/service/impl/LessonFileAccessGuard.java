package com.vierec.modules.course.service.impl;

import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.security.SecurityUtils;
import com.vierec.modules.course.entity.EnrollmentStatus;
import com.vierec.modules.course.repository.CourseEnrollmentRepository;
import com.vierec.modules.course.repository.LessonRepository;
import com.vierec.modules.file.service.FileAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** A lesson file can only be read by admins and learners whose enrollment in that course was approved. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonFileAccessGuard implements FileAccessGuard {

    private final LessonRepository lessonRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    @Override
    public void checkRead(Long fileId, Authentication authentication) {
        if (SecurityUtils.isAdmin(authentication)) {
            return;
        }
        List<Long> courseIds = lessonRepository.findCourseIdsByFileId(fileId);
        if (courseIds.isEmpty()) {
            return;
        }
        boolean learning = authentication != null
                && enrollmentRepository.existsByUserUsernameAndCourseIdInAndStatusIn(authentication.getName(),
                        courseIds, EnrollmentStatus.LEARNING);
        if (!learning) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
