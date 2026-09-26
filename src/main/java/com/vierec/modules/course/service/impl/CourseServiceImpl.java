package com.vierec.modules.course.service.impl;

import com.vierec.common.dto.PageResponse;
import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.common.exception.ResourceNotFoundException;
import com.vierec.modules.course.dto.CourseDetailResponse;
import com.vierec.modules.course.dto.CourseResponse;
import com.vierec.modules.course.dto.CourseRequest;
import com.vierec.modules.course.entity.Course;
import com.vierec.modules.course.entity.CourseStatus;
import com.vierec.modules.course.mapper.CourseMapper;
import com.vierec.modules.course.repository.CourseRepository;
import com.vierec.modules.course.repository.LessonRepository;
import com.vierec.modules.course.service.CourseService;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final CourseMapper courseMapper;

    @Override
    public PageResponse<CourseResponse> list(String keyword, CourseStatus status, int page, boolean publishedOnly) {
        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : null;
        CourseStatus effectiveStatus = publishedOnly ? CourseStatus.PUBLISHED : status;

        Page<Course> courses = courseRepository.search(normalized, effectiveStatus,
                PageRequest.of(page, PAGE_SIZE, NEWEST_FIRST));
        Map<Long, Long> lessonCounts = lessonCounts(courses.getContent());
        return PageResponse.of(courses,
                course -> courseMapper.toResponse(course, lessonCounts.getOrDefault(course.getId(), 0L)));
    }

    @Override
    public CourseDetailResponse get(Long id, boolean publishedOnly) {
        Course course = courseRepository.findDetailById(id)
                .filter(found -> !publishedOnly || found.getStatus() == CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND));
        return courseMapper.toDetailResponse(course, lessonRepository.findAllWithFilesByCourseId(id));
    }

    @Override
    @Transactional
    public CourseResponse create(CourseRequest request, String creatorUsername) {
        User instructor = activeInstructor(request.getInstructorId());
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        Course course = new Course();
        apply(course, request, instructor);
        course.setCreatedBy(creator);

        Course saved = courseRepository.save(course);
        log.info("Created course id={} by user id={}", saved.getId(), creator.getId());
        return courseMapper.toResponse(saved, 0L);
    }

    @Override
    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        Course course = findEntityById(id);
        // Only a newly assigned instructor must be active; keeping a now-inactive one is allowed.
        User current = course.getInstructor();
        User instructor = current != null && current.getId().equals(request.getInstructorId())
                ? current : activeInstructor(request.getInstructorId());
        apply(course, request, instructor);

        Course saved = courseRepository.saveAndFlush(course);
        log.info("Updated course id={}", id);
        return courseMapper.toResponse(saved, lessonCounts(Collections.singletonList(saved))
                .getOrDefault(id, 0L));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Course course = findEntityById(id);
        course.setDeletedAt(LocalDateTime.now());
        courseRepository.save(course);
        log.info("Soft-deleted course id={}", id);
    }

    private Course findEntityById(Long id) {
        return courseRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND));
    }

    private User activeInstructor(Long instructorId) {
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INSTRUCTOR_NOT_FOUND));
        if (!instructor.isActive()) {
            throw new BusinessException(ErrorCode.INSTRUCTOR_NOT_ACTIVE);
        }
        return instructor;
    }

    private static void apply(Course course, CourseRequest request, User instructor) {
        course.setName(request.getName().trim());
        course.setDescription(request.getDescription().trim());
        course.setInstructor(instructor);
        course.setStatus(request.getStatus());
    }

    /** One grouped query for the whole page instead of one count per course. */
    private Map<Long, Long> lessonCounts(List<Course> courses) {
        if (courses.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = courses.stream().map(Course::getId).collect(Collectors.toList());
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : lessonRepository.countByCourseIds(ids)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }
}
