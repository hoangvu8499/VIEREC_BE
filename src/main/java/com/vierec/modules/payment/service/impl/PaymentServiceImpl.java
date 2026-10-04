package com.vierec.modules.payment.service.impl;

import com.vierec.common.dto.PageResponse;
import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.common.exception.ResourceNotFoundException;
import com.vierec.modules.course.entity.Course;
import com.vierec.modules.course.repository.CourseRepository;
import com.vierec.modules.payment.dto.CreatePaymentRequest;
import com.vierec.modules.payment.dto.PaymentDetails;
import com.vierec.modules.payment.dto.PaymentResponse;
import com.vierec.modules.payment.entity.Payment;
import com.vierec.modules.payment.entity.PaymentStatus;
import com.vierec.modules.payment.mapper.PaymentMapper;
import com.vierec.modules.payment.repository.PaymentRepository;
import com.vierec.modules.payment.service.PaymentService;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponse create(CreatePaymentRequest request, String actorUsername) {
        User payer = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        Course course = courseRepository.findDetailById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND));
        User actor = userRepository.findByUsername(actorUsername)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        Payment payment = new Payment();
        payment.setUser(payer);
        payment.setCourse(course);
        payment.setCourseName(course.getName());
        payment.setAmount(request.getAmount());
        payment.setCreatedBy(actor);
        apply(payment, request, request.getStatus() == null ? PaymentStatus.PENDING : request.getStatus());

        Payment saved = paymentRepository.save(payment);
        log.info("User id={} recorded payment id={} of user id={} for course id={} ({})",
                actor.getId(), saved.getId(), payer.getId(), course.getId(), saved.getStatus());
        return paymentMapper.toResponse(saved);
    }

    @Override
    public PageResponse<PaymentResponse> search(Long userId, Long courseId, PaymentStatus status, int page,
                                                int size) {
        return PageResponse.of(paymentRepository.search(userId, courseId, status,
                PageRequest.of(page, size, NEWEST_FIRST)), paymentMapper::toResponse);
    }

    @Override
    public PageResponse<PaymentResponse> listMine(String username, PaymentStatus status, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        return search(user.getId(), null, status, page, size);
    }

    @Override
    public PaymentResponse get(Long id, String username, boolean admin) {
        Payment payment = findEntity(id);
        if (!admin && !payment.getUser().getUsername().equals(username)) {
            // Same answer as a missing id, so other users' payment ids cannot be probed.
            throw new ResourceNotFoundException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse update(Long id, PaymentDetails request) {
        Payment payment = findEntity(id);
        PaymentStatus status = request.getStatus() == null ? payment.getStatus() : request.getStatus();
        if (status == PaymentStatus.REFUNDED && payment.getStatus() != PaymentStatus.PAID
                && payment.getStatus() != PaymentStatus.REFUNDED) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_REFUNDABLE);
        }
        apply(payment, request, status);

        Payment saved = paymentRepository.saveAndFlush(payment);
        log.info("Updated payment id={} ({})", id, saved.getStatus());
        return paymentMapper.toResponse(saved);
    }

    private Payment findEntity(Long id) {
        return paymentRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    /** paid_at is only kept for PAID / REFUNDED; the sent value wins, otherwise the current one or now. */
    private static void apply(Payment payment, PaymentDetails request, PaymentStatus status) {
        payment.setMethod(request.getMethod());
        payment.setStatus(status);
        payment.setTransactionRef(trimToNull(request.getTransactionRef()));
        payment.setNote(trimToNull(request.getNote()));
        if (status == PaymentStatus.PAID || status == PaymentStatus.REFUNDED) {
            if (request.getPaidAt() != null) {
                payment.setPaidAt(request.getPaidAt());
            } else if (payment.getPaidAt() == null) {
                payment.setPaidAt(LocalDateTime.now());
            }
        } else {
            payment.setPaidAt(null);
        }
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
