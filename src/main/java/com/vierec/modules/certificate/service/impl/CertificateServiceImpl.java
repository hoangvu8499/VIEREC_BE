package com.vierec.modules.certificate.service.impl;

import com.vierec.common.dto.PageResponse;
import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.common.exception.ResourceNotFoundException;
import com.vierec.modules.certificate.dto.CertificateResponse;
import com.vierec.modules.certificate.dto.CertificateVerificationResponse;
import com.vierec.modules.certificate.entity.Certificate;
import com.vierec.modules.certificate.mapper.CertificateMapper;
import com.vierec.modules.certificate.repository.CertificateRepository;
import com.vierec.modules.certificate.service.CertificateService;
import com.vierec.modules.course.entity.Course;
import com.vierec.modules.course.entity.CourseEnrollment;
import com.vierec.modules.course.entity.EnrollmentStatus;
import com.vierec.modules.course.repository.CourseEnrollmentRepository;
import com.vierec.modules.course.repository.CourseRepository;
import com.vierec.modules.file.service.FileAccessGuard;
import com.vierec.modules.file.service.FileCategory;
import com.vierec.modules.file.service.FileStorageService;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.repository.UserRepository;
import com.vierec.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateServiceImpl implements CertificateService, FileAccessGuard {

    /** Sub-folder of {@code app.upload.dir} for certificate PDFs. */
    private static final String UPLOAD_FOLDER = "certificates";
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("issuedAt"), Sort.Order.desc("id"));
    /** No 0/O and 1/I, so a code read off paper is not mistyped. */
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_RANDOM_LENGTH = 8;
    private static final int CODE_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CertificateRepository certificateRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final CertificateMapper certificateMapper;

    @Override
    @Transactional
    public CertificateResponse issue(Long courseId, Long enrollmentId, MultipartFile pdf, String actorUsername) {
        Course course = courseRepository.findDetailById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND));
        CourseEnrollment enrollment = enrollmentRepository.findByIdAndCourseId(enrollmentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ENROLLMENT_NOT_FOUND));
        if (certificateRepository.existsByEnrollmentId(enrollmentId)) {
            throw new BusinessException(ErrorCode.CERTIFICATE_ALREADY_ISSUED);
        }
        if (enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ENROLLMENT_NOT_COMPLETED);
        }
        User actor = userRepository.findByUsername(actorUsername)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        User trainee = enrollment.getUser();

        Certificate certificate = new Certificate();
        certificate.setCode(newCode());
        certificate.setEnrollment(enrollment);
        certificate.setCourseName(course.getName());
        certificate.setFullName((trainee.getLastName() + " " + trainee.getFirstName()).trim());
        certificate.setDateOfBirth(trainee.getDateOfBirth());
        certificate.setCccd(trainee.getCccd());
        certificate.setIssuedBy(actor);
        // Stored last: every check above runs before anything is written to disk.
        certificate.setFile(fileStorageService.store(pdf, FileCategory.CERTIFICATE, UPLOAD_FOLDER));

        Certificate saved = certificateRepository.saveAndFlush(certificate);
        log.info("User id={} issued certificate {} for enrollment id={}", actor.getId(), saved.getCode(),
                enrollmentId);
        return certificateMapper.toResponse(saved);
    }

    @Override
    public PageResponse<CertificateResponse> listMine(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        return PageResponse.of(certificateRepository.findByUserId(user.getId(),
                PageRequest.of(page, size, NEWEST_FIRST)), certificateMapper::toResponse);
    }

    @Override
    public CertificateResponse getMine(String username, String code) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        return certificateRepository.findByCodeAndUserId(CertificateService.normalizeCode(code), user.getId())
                .map(certificateMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CERTIFICATE_NOT_FOUND));
    }

    @Override
    public CertificateVerificationResponse verify(String code) {
        return certificateRepository.findByCode(CertificateService.normalizeCode(code))
                .map(certificateMapper::toVerification)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CERTIFICATE_NOT_FOUND));
    }

    /** A certificate PDF is private: only its owner, the managers of the owner's business and admins. */
    @Override
    public void checkRead(Long fileId, Authentication authentication) {
        List<Object[]> owner = certificateRepository.findOwnerByFileId(fileId);
        if (owner.isEmpty() || SecurityUtils.isAdmin(authentication)) {
            return;
        }
        Long ownerId = (Long) owner.get(0)[0];
        Long businessId = (Long) owner.get(0)[1];
        boolean allowed = authentication != null && userRepository.findByUsername(authentication.getName())
                .map(user -> user.getId().equals(ownerId) || (businessId != null && user.hasRole(RoleCode.BUSINESS)
                        && user.getBusiness() != null && businessId.equals(user.getBusiness().getId())))
                .orElse(false);
        if (!allowed) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    /** {@code VRC-<year>-<8 random characters>}: random so that the public lookup cannot be enumerated. */
    private String newCode() {
        for (int attempt = 0; attempt < CODE_ATTEMPTS; attempt++) {
            StringBuilder code = new StringBuilder("VRC-").append(LocalDate.now().getYear()).append('-');
            for (int i = 0; i < CODE_RANDOM_LENGTH; i++) {
                code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            if (!certificateRepository.existsByCode(code.toString())) {
                return code.toString();
            }
        }
        throw new IllegalStateException("Could not generate a unique certificate code");
    }
}
