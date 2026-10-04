package com.vierec.modules.course.service.impl;

import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.common.exception.ResourceNotFoundException;
import com.vierec.modules.course.dto.CourseProgressResponse;
import com.vierec.modules.course.dto.VideoProgressRequest;
import com.vierec.modules.course.entity.CourseEnrollment;
import com.vierec.modules.course.entity.CourseVideos;
import com.vierec.modules.course.entity.EnrollmentStatus;
import com.vierec.modules.course.entity.Lesson;
import com.vierec.modules.course.entity.LessonProgress;
import com.vierec.modules.course.mapper.CourseMapper;
import com.vierec.modules.course.repository.CourseEnrollmentRepository;
import com.vierec.modules.course.repository.CourseRepository;
import com.vierec.modules.course.repository.LessonProgressRepository;
import com.vierec.modules.course.repository.LessonRepository;
import com.vierec.modules.course.service.LearningProgressService;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearningProgressServiceImpl implements LearningProgressService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final CourseMapper courseMapper;

    @Override
    public CourseProgressResponse getMine(Long courseId, String username) {
        User user = learner(courseId, username);
        return summarize(user.getId(), courseId);
    }

    @Override
    @Transactional
    public CourseProgressResponse saveMine(Long courseId, String username, List<VideoProgressRequest> videos) {
        User user = learner(courseId, username);
        List<Lesson> lessons = lessonRepository.findAllWithFilesByCourseId(courseId);
        Map<Long, Lesson> lessonsById = lessons.stream().collect(Collectors.toMap(Lesson::getId, lesson -> lesson));
        Map<String, LessonProgress> rows = new HashMap<>();
        progressRepository.findByUserAndCourse(user.getId(), courseId)
                .forEach(row -> rows.put(key(row.getLesson().getId(), row.getVideoKey()), row));

        int saved = 0;
        for (VideoProgressRequest video : videos) {
            Lesson lesson = lessonsById.get(video.getLessonId());
            if (lesson == null || !CourseVideos.keys(lesson).contains(video.getVideoKey())) {
                continue;
            }
            LessonProgress row = rows.computeIfAbsent(key(lesson.getId(), video.getVideoKey()), ignored -> {
                LessonProgress created = new LessonProgress();
                created.setUser(user);
                created.setLesson(lesson);
                created.setVideoKey(video.getVideoKey());
                return created;
            });
            if (video.getDurationSeconds() > 0) {
                row.setDurationSeconds(video.getDurationSeconds());
            }
            int cap = row.getDurationSeconds() > 0 ? row.getDurationSeconds() : video.getWatchedSeconds();
            row.setWatchedSeconds(Math.max(row.getWatchedSeconds(), Math.min(video.getWatchedSeconds(), cap)));
            progressRepository.save(row);
            saved++;
        }
        progressRepository.flush();
        log.debug("User id={} saved progress of {} videos in course id={}", user.getId(), saved, courseId);
        return summarize(lessons, new ArrayList<>(rows.values()));
    }

    @Override
    public CourseProgressResponse summarize(Long userId, Long courseId) {
        return summarize(lessonRepository.findAllWithFilesByCourseId(courseId),
                progressRepository.findByUserAndCourse(userId, courseId));
    }

    @Override
    public Map<Long, CourseProgressResponse> summarizeAll(Long userId, Collection<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<Lesson>> lessonsByCourse = lessonRepository.findAllWithFilesByCourseIds(courseIds).stream()
                .collect(Collectors.groupingBy(lesson -> lesson.getCourse().getId()));
        List<LessonProgress> rows = progressRepository.findByUserAndCourses(userId, courseIds);
        Map<Long, CourseProgressResponse> summaries = new HashMap<>();
        for (Long courseId : courseIds) {
            CourseProgressResponse summary = summarize(
                    lessonsByCourse.getOrDefault(courseId, Collections.<Lesson>emptyList()), rows);
            summary.setVideos(null);
            summaries.put(courseId, summary);
        }
        return summaries;
    }

    /**
     * Same rule as the learner's page: a video never opened has no known length, so the exam stays closed until
     * every video was opened and 80% of the total length was watched. Rows of deleted lessons or replaced videos
     * do not count.
     */
    private CourseProgressResponse summarize(List<Lesson> lessons, List<LessonProgress> rows) {
        Map<String, LessonProgress> byKey = new HashMap<>();
        rows.forEach(row -> byKey.put(key(row.getLesson().getId(), row.getVideoKey()), row));

        int videoCount = 0;
        int unopened = 0;
        long total = 0;
        long watched = 0;
        List<LessonProgress> counted = new ArrayList<>();
        for (Lesson lesson : lessons) {
            for (String videoKey : CourseVideos.keys(lesson)) {
                videoCount++;
                LessonProgress row = byKey.get(key(lesson.getId(), videoKey));
                if (row != null) {
                    counted.add(row);
                }
                if (row == null || row.getDurationSeconds() == 0) {
                    unopened++;
                    continue;
                }
                total += row.getDurationSeconds();
                watched += Math.min(row.getWatchedSeconds(), row.getDurationSeconds());
            }
        }
        double ratio = total > 0 ? (double) watched / total : 0;
        return CourseProgressResponse.builder()
                .videoCount(videoCount)
                .unopenedCount(unopened)
                .totalSeconds(total)
                .watchedSeconds(watched)
                .percent(videoCount == 0 ? null : (int) Math.min(100, Math.floor(ratio * 100)))
                .examReady(videoCount == 0 || (unopened == 0 && ratio >= EXAM_WATCH_RATIO))
                .videos(counted.stream().map(courseMapper::toVideoProgress).collect(Collectors.toList()))
                .build();
    }

    private User learner(Long courseId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND);
        }
        boolean learning = enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .map(CourseEnrollment::getStatus)
                .filter(EnrollmentStatus.LEARNING::contains)
                .isPresent();
        if (!learning) {
            throw new BusinessException(ErrorCode.PROGRESS_NOT_ALLOWED);
        }
        return user;
    }

    private static String key(Long lessonId, String videoKey) {
        return lessonId + ":" + videoKey;
    }
}
