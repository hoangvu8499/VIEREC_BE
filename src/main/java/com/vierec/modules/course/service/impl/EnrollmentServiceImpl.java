package com.vierec.modules.course.service.impl;

import com.vierec.common.dto.PageResponse;
import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.common.exception.ResourceNotFoundException;
import com.vierec.modules.certificate.repository.CertificateRepository;
import com.vierec.modules.course.dto.EnrollmentResponse;
import com.vierec.modules.course.dto.MonthlyRevenueResponse;
import com.vierec.modules.course.dto.RevenueResponse;
import com.vierec.modules.course.entity.Course;
import com.vierec.modules.course.entity.CourseEnrollment;
import com.vierec.modules.course.entity.CourseStatus;
import com.vierec.modules.course.entity.EnrollmentStatus;
import com.vierec.modules.course.mapper.CourseMapper;
import com.vierec.modules.course.repository.CourseEnrollmentRepository;
import com.vierec.modules.course.repository.CourseRepository;
import com.vierec.modules.course.repository.LessonRepository;
import com.vierec.modules.course.service.EnrollResult;
import com.vierec.modules.course.service.EnrollmentService;
import com.vierec.modules.course.service.GroupEnrollResult;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentServiceImpl implements EnrollmentService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("enrolledAt"), Sort.Order.desc("id"));
    private static final Sort LATEST_APPROVAL_FIRST = Sort.by(Sort.Order.desc("approvedAt"), Sort.Order.desc("id"));
    private static final List<EnrollmentStatus> ACTIVE_STATUSES =
            Arrays.asList(EnrollmentStatus.PENDING, EnrollmentStatus.ENROLLED, EnrollmentStatus.COMPLETED);

    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final CertificateRepository certificateRepository;
    private final CourseMapper courseMapper;

    @Override
    @Transactional
    public EnrollResult enroll(Long courseId, String username) {
        return enroll(publishedCourse(courseId), findUser(username));
    }

    @Override
    @Transactional
    public GroupEnrollResult enrollAll(Long courseId, List<User> users) {
        Course course = publishedCourse(courseId);
        if (users.isEmpty()) {
            return new GroupEnrollResult(Collections.emptyList(), Collections.emptyList());
        }
        Map<Long, CourseEnrollment> current = new HashMap<>();
        enrollmentRepository.findByCourseIdAndUserIdIn(courseId,
                        users.stream().map(User::getId).collect(Collectors.toList()))
                .forEach(enrollment -> current.put(enrollment.getUser().getId(), enrollment));

        List<CourseEnrollment> requested = new ArrayList<>();
        List<CourseEnrollment> skipped = new ArrayList<>();
        for (User user : users) {
            CourseEnrollment enrollment = current.get(user.getId());
            if (enrollment != null && enrollment.getStatus() != EnrollmentStatus.CANCELLED) {
                skipped.add(enrollment);
            } else {
                requested.add(request(course, user, enrollment));
            }
        }
        enrollmentRepository.saveAll(requested);
        enrollmentRepository.flush();
        long lessonCount = lessonCounts(Collections.singleton(courseId)).getOrDefault(courseId, 0L);
        List<EnrollmentResponse> enrolled = requested.stream()
                .map(enrollment -> courseMapper.toEnrollmentResponse(enrollment, lessonCount))
                .collect(Collectors.toList());
        log.info("{} users requested course id={} ({} skipped)", requested.size(), courseId, skipped.size());
        return new GroupEnrollResult(enrolled, skipped);
    }

    private Course publishedCourse(Long courseId) {
        return courseRepository.findDetailById(courseId)
                .filter(found -> found.getStatus() == CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND));
    }

    private EnrollResult enroll(Course course, User user) {
        CourseEnrollment current = enrollmentRepository.findByUserIdAndCourseId(user.getId(), course.getId())
                .orElse(null);
        if (current != null && current.getStatus() != EnrollmentStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ALREADY_ENROLLED);
        }
        CourseEnrollment saved = enrollmentRepository.saveAndFlush(request(course, user, current));
        log.info("User id={} requested course id={} ({})", user.getId(), course.getId(),
                current == null ? "new" : "re-enrolled");
        return new EnrollResult(toResponse(saved), current == null);
    }

    /** A new PENDING enrollment at today's price, or the user's CANCELLED one ({@code current}) taken up again. */
    private static CourseEnrollment request(Course course, User user, CourseEnrollment current) {
        CourseEnrollment enrollment = current;
        if (enrollment == null) {
            enrollment = new CourseEnrollment();
            enrollment.setUser(user);
            enrollment.setCourse(course);
        } else {
            enrollment.setStatus(EnrollmentStatus.PENDING);
            enrollment.setEnrolledAt(LocalDateTime.now());
            enrollment.setCompletedAt(null);
        }
        enrollment.setPrice(course.getPrice());
        enrollment.setApprovedAt(null);
        return enrollment;
    }

    @Override
    @Transactional
    public void cancel(Long courseId, String username) {
        User user = findUser(username);
        CourseEnrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .filter(found -> found.getStatus() != EnrollmentStatus.CANCELLED)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ENROLLMENT_NOT_FOUND));
        if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ENROLLMENT_COMPLETED);
        }
        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        enrollmentRepository.save(enrollment);
        log.info("User id={} cancelled enrollment id={}", user.getId(), enrollment.getId());
    }

    @Override
    public PageResponse<EnrollmentResponse> listMine(String username, EnrollmentStatus status, int page, int size) {
        User user = findUser(username);
        Collection<EnrollmentStatus> statuses = status == null ? ACTIVE_STATUSES : Collections.singleton(status);
        return toPage(enrollmentRepository.findByUser(user.getId(), statuses,
                PageRequest.of(page, size, NEWEST_FIRST)));
    }

    @Override
    public PageResponse<EnrollmentResponse> listByCourse(Long courseId, EnrollmentStatus status, int page,
                                                         int size) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND);
        }
        return toPage(enrollmentRepository.findByCourse(courseId, status, PageRequest.of(page, size, NEWEST_FIRST)));
    }

    @Override
    public PageResponse<EnrollmentResponse> search(EnrollmentStatus status, String keyword, int page, int size) {
        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return toPage(enrollmentRepository.search(status, normalized, PageRequest.of(page, size, NEWEST_FIRST)));
    }

    @Override
    @Transactional
    public EnrollmentResponse updateStatus(Long courseId, Long enrollmentId, EnrollmentStatus status) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND);
        }
        CourseEnrollment enrollment = enrollmentRepository.findByIdAndCourseId(enrollmentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ENROLLMENT_NOT_FOUND));
        if (enrollment.getStatus() != status) {
            if (certificateRepository.existsByEnrollmentId(enrollmentId)) {
                throw new BusinessException(ErrorCode.CERTIFIED_ENROLLMENT_LOCKED);
            }
            LocalDateTime now = LocalDateTime.now();
            enrollment.setStatus(status);
            enrollment.setCompletedAt(status == EnrollmentStatus.COMPLETED ? now : null);
            // Approval is the moment the learner first got in; reopening a completed course keeps it.
            if (!EnrollmentStatus.LEARNING.contains(status)) {
                enrollment.setApprovedAt(null);
            } else if (enrollment.getApprovedAt() == null) {
                enrollment.setApprovedAt(now);
            }
            enrollment = enrollmentRepository.saveAndFlush(enrollment);
            log.info("Enrollment id={} of course id={} set to {}", enrollmentId, courseId, status);
        }
        return toResponse(enrollment);
    }

    @Override
    public RevenueResponse revenue(LocalDate today) {
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return RevenueResponse.builder()
                .week(revenueSince(weekStart))
                .month(revenueSince(today.withDayOfMonth(1)))
                .year(revenueSince(today.withDayOfYear(1)))
                .build();
    }

    @Override
    public MonthlyRevenueResponse monthlyRevenue(YearMonth month, String keyword, int page, int size) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : null;

        Object[] total = enrollmentRepository.sumApproved(EnrollmentStatus.LEARNING, from, to, null).get(0);
        Object[] matched = normalized == null ? total
                : enrollmentRepository.sumApproved(EnrollmentStatus.LEARNING, from, to, normalized).get(0);
        List<MonthlyRevenueResponse.CourseRevenue> courses = enrollmentRepository
                .sumApprovedByCourse(EnrollmentStatus.LEARNING, from, to).stream()
                .map(row -> new MonthlyRevenueResponse.CourseRevenue((Long) row[0], (String) row[1],
                        ((Number) row[2]).longValue(), ((Number) row[3]).longValue()))
                .collect(Collectors.toList());
        Page<CourseEnrollment> items = enrollmentRepository.findApproved(EnrollmentStatus.LEARNING, from, to,
                normalized, PageRequest.of(page, size, LATEST_APPROVAL_FIRST));

        return MonthlyRevenueResponse.builder()
                .from(month.atDay(1))
                .to(month.atEndOfMonth())
                .amount(((Number) total[0]).longValue())
                .enrollments(((Number) total[1]).longValue())
                .courses(courses)
                .matchedAmount(((Number) matched[0]).longValue())
                .items(toPage(items))
                .build();
    }

    private RevenueResponse.Period revenueSince(LocalDate from) {
        Object[] row = enrollmentRepository.sumApprovedSince(EnrollmentStatus.LEARNING, from.atStartOfDay()).get(0);
        return new RevenueResponse.Period(from, ((Number) row[0]).longValue(), ((Number) row[1]).longValue());
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    private EnrollmentResponse toResponse(CourseEnrollment enrollment) {
        Long courseId = enrollment.getCourse().getId();
        return courseMapper.toEnrollmentResponse(enrollment,
                lessonCounts(Collections.singleton(courseId)).getOrDefault(courseId, 0L));
    }

    private PageResponse<EnrollmentResponse> toPage(Page<CourseEnrollment> enrollments) {
        Set<Long> courseIds = enrollments.getContent().stream()
                .map(enrollment -> enrollment.getCourse().getId())
                .collect(Collectors.toSet());
        Map<Long, Long> counts = lessonCounts(courseIds);
        return PageResponse.of(enrollments, enrollment -> courseMapper.toEnrollmentResponse(enrollment,
                counts.getOrDefault(enrollment.getCourse().getId(), 0L)));
    }

    /** One grouped query for the whole page instead of one count per course. */
    private Map<Long, Long> lessonCounts(Collection<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : lessonRepository.countByCourseIds(courseIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }
}
