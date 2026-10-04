package com.vierec.modules.business.service.impl;

import com.vierec.common.dto.PageResponse;
import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.common.exception.ResourceNotFoundException;
import com.vierec.modules.auth.dto.RegisterRequest;
import com.vierec.modules.business.dto.BusinessEnrollRequest;
import com.vierec.modules.business.dto.BusinessEnrollResponse;
import com.vierec.modules.business.dto.BusinessManagerRequest;
import com.vierec.modules.business.dto.BusinessMemberDetailResponse;
import com.vierec.modules.business.dto.BusinessMemberResponse;
import com.vierec.modules.business.dto.BusinessRequest;
import com.vierec.modules.business.dto.BusinessResponse;
import com.vierec.modules.business.dto.CreateBusinessRequest;
import com.vierec.modules.business.dto.MemberCourseResponse;
import com.vierec.modules.business.entity.Business;
import com.vierec.modules.business.entity.BusinessStatus;
import com.vierec.modules.business.mapper.BusinessMapper;
import com.vierec.modules.business.repository.BusinessRepository;
import com.vierec.modules.business.service.BusinessService;
import com.vierec.modules.certificate.entity.Certificate;
import com.vierec.modules.certificate.dto.CertificateResponse;
import com.vierec.modules.certificate.mapper.CertificateMapper;
import com.vierec.modules.certificate.repository.CertificateRepository;
import com.vierec.modules.certificate.service.CertificateService;
import com.vierec.modules.course.dto.CourseProgressResponse;
import com.vierec.modules.course.dto.EnrollmentResponse;
import com.vierec.modules.course.entity.Course;
import com.vierec.modules.course.entity.CourseEnrollment;
import com.vierec.modules.course.entity.CourseStatus;
import com.vierec.modules.course.entity.EnrollmentStatus;
import com.vierec.modules.course.repository.CourseEnrollmentRepository;
import com.vierec.modules.course.repository.CourseRepository;
import com.vierec.modules.course.service.EnrollmentService;
import com.vierec.modules.course.service.GroupEnrollResult;
import com.vierec.modules.course.service.LearningProgressService;
import com.vierec.modules.exam.entity.ExamAttempt;
import com.vierec.modules.exam.repository.ExamAttemptRepository;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.user.dto.UserResponse;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.mapper.UserMapper;
import com.vierec.modules.user.repository.UserRepository;
import com.vierec.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessServiceImpl implements BusinessService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private static final Sort LATEST_CERTIFICATE_FIRST = Sort.by(Sort.Order.desc("issuedAt"), Sort.Order.desc("id"));

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CertificateRepository certificateRepository;
    private final ExamAttemptRepository attemptRepository;
    private final UserService userService;
    private final EnrollmentService enrollmentService;
    private final LearningProgressService progressService;
    private final BusinessMapper businessMapper;
    private final CertificateMapper certificateMapper;
    private final UserMapper userMapper;

    // ---------------------------------------------------------------- admin

    @Override
    public PageResponse<BusinessResponse> search(String keyword, BusinessStatus status, int page, int size) {
        Page<Business> businesses = businessRepository.search(normalize(keyword), status,
                PageRequest.of(page, size, NEWEST_FIRST));
        List<Long> ids = businesses.getContent().stream().map(Business::getId).collect(Collectors.toList());
        Map<Long, Long> managers = countsById(ids.isEmpty() ? Collections.<Object[]>emptyList()
                : userRepository.countManagers(ids));
        Map<Long, Long> members = countsById(ids.isEmpty() ? Collections.<Object[]>emptyList()
                : userRepository.countMembers(ids));
        return PageResponse.of(businesses, business -> businessMapper.toResponse(business,
                managers.getOrDefault(business.getId(), 0L), members.getOrDefault(business.getId(), 0L)));
    }

    @Override
    @Transactional
    public BusinessResponse create(CreateBusinessRequest request, String actorUsername) {
        String taxCode = request.getTaxCode().trim();
        if (businessRepository.existsByTaxCode(taxCode)) {
            throw new BusinessException(ErrorCode.TAX_CODE_ALREADY_EXISTS);
        }
        Business business = new Business();
        apply(business, request);
        if (request.getStatus() != null) {
            business.setStatus(request.getStatus());
        }
        business = businessRepository.saveAndFlush(business);
        // Same transaction: a taken username / email / phone rolls the business back too.
        userService.createBusinessAccount(request.getManager().toRegisterRequest(), RoleCode.BUSINESS, business,
                actorUsername);
        log.info("Created business id={} ({})", business.getId(), business.getTaxCode());
        return toResponse(business);
    }

    @Override
    public BusinessResponse get(Long id) {
        return toResponse(findBusiness(id));
    }

    @Override
    @Transactional
    public BusinessResponse update(Long id, BusinessRequest request) {
        Business business = findBusiness(id);
        if (businessRepository.existsByTaxCodeAndIdNot(request.getTaxCode().trim(), id)) {
            throw new BusinessException(ErrorCode.TAX_CODE_ALREADY_EXISTS);
        }
        apply(business, request);
        if (request.getStatus() != null) {
            business.setStatus(request.getStatus());
        }
        business = businessRepository.saveAndFlush(business);
        log.info("Updated business id={} (status {})", id, business.getStatus());
        return toResponse(business);
    }

    @Override
    public List<UserResponse> managers(Long id) {
        findBusiness(id);
        return userRepository.findManagers(id).stream().map(userMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse addManager(Long id, BusinessManagerRequest request, String actorUsername) {
        Business business = findBusiness(id);
        return userMapper.toResponse(userService.createBusinessAccount(request.toRegisterRequest(),
                RoleCode.BUSINESS, business, actorUsername));
    }

    @Override
    public PageResponse<BusinessMemberResponse> members(Long id, String keyword, int page, int size) {
        return listMembers(findBusiness(id), keyword, page, size);
    }

    @Override
    public BusinessMemberDetailResponse member(Long id, Long userId) {
        return memberDetail(findBusiness(id), userId);
    }

    // ---------------------------------------------------------------- business manager

    @Override
    public BusinessResponse mine(String username) {
        return toResponse(managedBusiness(username));
    }

    @Override
    public PageResponse<BusinessMemberResponse> myMembers(String username, String keyword, int page, int size) {
        return listMembers(managedBusiness(username), keyword, page, size);
    }

    @Override
    @Transactional
    public BusinessMemberResponse createMember(String username, RegisterRequest request) {
        Business business = managedBusiness(username);
        User member = userService.createBusinessAccount(request, RoleCode.TRAINEE, business, username);
        return businessMapper.toMember(member);
    }

    @Override
    public BusinessMemberDetailResponse myMember(String username, Long userId) {
        return memberDetail(managedBusiness(username), userId);
    }

    @Override
    @Transactional
    public BusinessEnrollResponse enroll(String username, BusinessEnrollRequest request) {
        Business business = managedBusiness(username);
        Course course = courseRepository.findDetailById(request.getCourseId())
                .filter(found -> found.getStatus() == CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND));
        List<User> members = userRepository.findMembersByIds(business.getId(), request.getUserIds());
        if (members.size() != request.getUserIds().size()) {
            throw new BusinessException(ErrorCode.BUSINESS_ENROLL_NOT_MEMBERS);
        }
        members.sort(Comparator.comparing(User::getLastName).thenComparing(User::getFirstName));

        GroupEnrollResult result = enrollmentService.enrollAll(course.getId(), members);
        List<EnrollmentResponse> enrolled = result.getEnrolled();
        List<BusinessEnrollResponse.Skipped> skipped = result.getSkipped().stream()
                .map(enrollment -> BusinessEnrollResponse.Skipped.builder()
                        .userId(enrollment.getUser().getId())
                        .fullName(fullName(enrollment.getUser()))
                        .status(enrollment.getStatus())
                        .build())
                .collect(Collectors.toList());
        long total = enrolled.stream().mapToLong(EnrollmentResponse::getPrice).sum();
        log.info("Business id={} enrolled {} learners in course id={} ({} skipped, {} VND)", business.getId(),
                enrolled.size(), course.getId(), skipped.size(), total);
        return BusinessEnrollResponse.builder()
                .courseId(course.getId())
                .courseName(course.getName())
                .enrolled(enrolled)
                .skipped(skipped)
                .totalAmount(total)
                .build();
    }

    @Override
    public PageResponse<CertificateResponse> myCertificates(String username, String keyword, int page, int size) {
        Business business = managedBusiness(username);
        return PageResponse.of(certificateRepository.findByBusinessId(business.getId(), normalize(keyword),
                PageRequest.of(page, size, LATEST_CERTIFICATE_FIRST)), certificateMapper::toResponse);
    }

    @Override
    public CertificateResponse myCertificate(String username, String code) {
        Business business = managedBusiness(username);
        return certificateRepository.findByCodeAndBusinessId(CertificateService.normalizeCode(code), business.getId())
                .map(certificateMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CERTIFICATE_NOT_FOUND));
    }

    // ---------------------------------------------------------------- helpers

    private PageResponse<BusinessMemberResponse> listMembers(Business business, String keyword, int page,
                                                             int size) {
        Page<User> users = userRepository.findMembers(business.getId(), normalize(keyword),
                PageRequest.of(page, size, NEWEST_FIRST));
        List<Long> ids = users.getContent().stream().map(User::getId).collect(Collectors.toList());
        Map<Long, Map<EnrollmentStatus, Long>> statuses = new HashMap<>();
        Map<Long, Long> certificates = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : enrollmentRepository.countByUsersAndStatus(ids)) {
                statuses.computeIfAbsent((Long) row[0], ignored -> new HashMap<>())
                        .put((EnrollmentStatus) row[1], (Long) row[2]);
            }
            certificates = countsById(certificateRepository.countByUserIds(ids));
        }
        Map<Long, Long> certificateCounts = certificates;
        return PageResponse.of(users, user -> {
            BusinessMemberResponse member = businessMapper.toMember(user);
            Map<EnrollmentStatus, Long> counts = statuses.getOrDefault(user.getId(), Collections.emptyMap());
            member.setPendingCount(counts.getOrDefault(EnrollmentStatus.PENDING, 0L));
            member.setLearningCount(counts.getOrDefault(EnrollmentStatus.ENROLLED, 0L));
            member.setCompletedCount(counts.getOrDefault(EnrollmentStatus.COMPLETED, 0L));
            member.setCertificateCount(certificateCounts.getOrDefault(user.getId(), 0L));
            return member;
        });
    }

    private BusinessMemberDetailResponse memberDetail(Business business, Long userId) {
        User user = userRepository.findMembersByIds(business.getId(), Collections.singleton(userId)).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BUSINESS_MEMBER_NOT_FOUND));
        List<CourseEnrollment> enrollments = enrollmentRepository.findAllByUserWithCourse(userId);
        Map<Long, ExamAttempt> attempts = attemptRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(attempt -> attempt.getExam().getCourse().getId(), Function.identity()));
        Map<Long, Certificate> certificates = certificateRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(certificate -> certificate.getEnrollment().getId(), Function.identity()));

        Map<Long, CourseProgressResponse> progress = progressService.summarizeAll(userId, enrollments.stream()
                .filter(enrollment -> EnrollmentStatus.LEARNING.contains(enrollment.getStatus()))
                .map(enrollment -> enrollment.getCourse().getId())
                .collect(Collectors.toList()));

        List<MemberCourseResponse> courses = new ArrayList<>();
        Map<EnrollmentStatus, Long> counts = new HashMap<>();
        for (CourseEnrollment enrollment : enrollments) {
            Long courseId = enrollment.getCourse().getId();
            MemberCourseResponse course = businessMapper.toCourse(enrollment);
            course.setProgress(progress.get(courseId));
            ExamAttempt attempt = attempts.get(courseId);
            course.setExam(attempt == null ? null : businessMapper.toExam(attempt));
            Certificate certificate = certificates.get(enrollment.getId());
            course.setCertificate(certificate == null ? null : businessMapper.toCertificate(certificate));
            courses.add(course);
            counts.merge(enrollment.getStatus(), 1L, Long::sum);
        }
        BusinessMemberResponse member = businessMapper.toMember(user);
        member.setPendingCount(counts.getOrDefault(EnrollmentStatus.PENDING, 0L));
        member.setLearningCount(counts.getOrDefault(EnrollmentStatus.ENROLLED, 0L));
        member.setCompletedCount(counts.getOrDefault(EnrollmentStatus.COMPLETED, 0L));
        member.setCertificateCount(certificates.size());
        return BusinessMemberDetailResponse.builder().member(member).courses(courses).build();
    }

    private Business managedBusiness(String username) {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        Business business = manager.getBusiness();
        if (!manager.hasRole(RoleCode.BUSINESS) || business == null) {
            throw new BusinessException(ErrorCode.NOT_A_BUSINESS_MANAGER);
        }
        if (business.getStatus() != BusinessStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.BUSINESS_INACTIVE);
        }
        return business;
    }

    private Business findBusiness(Long id) {
        return businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));
    }

    private BusinessResponse toResponse(Business business) {
        Collection<Long> ids = Collections.singleton(business.getId());
        return businessMapper.toResponse(business,
                countsById(userRepository.countManagers(ids)).getOrDefault(business.getId(), 0L),
                countsById(userRepository.countMembers(ids)).getOrDefault(business.getId(), 0L));
    }

    private static void apply(Business business, BusinessRequest request) {
        business.setName(request.getName().trim());
        business.setTaxCode(request.getTaxCode().trim());
        business.setAddress(request.getAddress().trim());
        business.setPhoneNumber(request.getPhoneNumber().trim());
        business.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
    }

    /** Rows of [id, count] to a map. */
    private static Map<Long, Long> countsById(List<Object[]> rows) {
        Map<Long, Long> counts = new HashMap<>();
        rows.forEach(row -> counts.put((Long) row[0], (Long) row[1]));
        return counts;
    }

    private static String normalize(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private static String fullName(User user) {
        return (user.getLastName() + " " + user.getFirstName()).trim();
    }
}
