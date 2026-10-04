package com.vierec.modules.exam.service.impl;

import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.common.exception.ResourceNotFoundException;
import com.vierec.modules.course.entity.CourseEnrollment;
import com.vierec.modules.course.entity.EnrollmentStatus;
import com.vierec.modules.course.repository.CourseEnrollmentRepository;
import com.vierec.modules.course.repository.CourseRepository;
import com.vierec.modules.course.service.LearningProgressService;
import com.vierec.modules.exam.dto.ExamAnswerRequest;
import com.vierec.modules.exam.dto.ExamAttemptResponse;
import com.vierec.modules.exam.dto.MyExamResponse;
import com.vierec.modules.exam.entity.Exam;
import com.vierec.modules.exam.entity.ExamAttempt;
import com.vierec.modules.exam.entity.ExamOption;
import com.vierec.modules.exam.entity.ExamQuestion;
import com.vierec.modules.exam.mapper.ExamMapper;
import com.vierec.modules.exam.repository.ExamAttemptRepository;
import com.vierec.modules.exam.repository.ExamRepository;
import com.vierec.modules.exam.service.ExamAttemptService;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
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
public class ExamAttemptServiceImpl implements ExamAttemptService {

    /** Answers sent this long after the deadline still count: the auto-submit at 00:00 needs time to arrive. */
    static final Duration SUBMIT_GRACE = Duration.ofMinutes(1);
    private static final BigDecimal MAX_SCORE = BigDecimal.TEN;

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository attemptRepository;
    private final ExamMapper examMapper;
    private final LearningProgressService progressService;

    @Override
    @Transactional
    public MyExamResponse get(Long courseId, String username) {
        Taker taker = taker(courseId, username);
        LocalDateTime now = LocalDateTime.now();
        return response(taker.exam, current(taker, now), now);
    }

    @Override
    @Transactional
    public MyExamResponse start(Long courseId, String username) {
        Taker taker = taker(courseId, username);
        LocalDateTime now = LocalDateTime.now();
        ExamAttempt attempt = current(taker, now);
        if (attempt != null) {
            return response(taker.exam, attempt, now);
        }
        if (taker.exam.getQuestions().isEmpty()) {
            throw new BusinessException(ErrorCode.EXAM_HAS_NO_QUESTIONS);
        }
        // A completed course (passed, or confirmed by an admin) needs no more watching.
        if (taker.enrollment.getStatus() != EnrollmentStatus.COMPLETED
                && !progressService.summarize(taker.user.getId(), courseId).isExamReady()) {
            throw new BusinessException(ErrorCode.EXAM_PROGRESS_NOT_ENOUGH);
        }
        attempt = new ExamAttempt();
        attempt.setExam(taker.exam);
        attempt.setUser(taker.user);
        attempt.setStartedAt(now);
        attempt.setDeadlineAt(now.plusMinutes(taker.exam.getDurationMinutes()));
        attempt.setPassScore(taker.exam.getPassScore());
        attempt = attemptRepository.saveAndFlush(attempt);
        log.info("User id={} started exam id={} (attempt id={})", taker.user.getId(), taker.exam.getId(),
                attempt.getId());
        return response(taker.exam, attempt, now);
    }

    @Override
    @Transactional
    public MyExamResponse submit(Long courseId, String username, List<ExamAnswerRequest> answers) {
        Taker taker = taker(courseId, username);
        LocalDateTime now = LocalDateTime.now();
        ExamAttempt attempt = attemptRepository.findForUpdate(taker.exam.getId(), taker.user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EXAM_ATTEMPT_NOT_FOUND));
        if (attempt.isSubmitted()) {
            throw new BusinessException(ErrorCode.EXAM_ALREADY_TAKEN);
        }
        Map<Long, ExamOption> selected = new HashMap<>();
        if (isLate(attempt, now)) {
            log.warn("Attempt id={} submitted {}s after its deadline: answers not counted", attempt.getId(),
                    Duration.between(attempt.getDeadlineAt(), now).getSeconds());
        } else {
            // The last answer of a question wins.
            answers.forEach(answer -> selected.put(answer.getQuestionId(), answer.getSelectedOption()));
        }
        grade(taker, attempt, selected, now);
        return response(taker.exam, attempt, now);
    }

    /** The user, the exam and the enrollment allowing them to take it. */
    private Taker taker(Long courseId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND);
        }
        CourseEnrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .filter(found -> EnrollmentStatus.LEARNING.contains(found.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_ALLOWED));
        Exam exam = examRepository.findWithQuestionsByCourseId(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EXAM_NOT_FOUND));
        return new Taker(user, enrollment, exam);
    }

    /** The attempt of the user, graded without answers when its time ran out and nothing was submitted. */
    private ExamAttempt current(Taker taker, LocalDateTime now) {
        ExamAttempt attempt = attemptRepository.findForUpdate(taker.exam.getId(), taker.user.getId()).orElse(null);
        if (attempt != null && !attempt.isSubmitted() && isLate(attempt, now)) {
            grade(taker, attempt, Collections.<Long, ExamOption>emptyMap(), now);
        }
        return attempt;
    }

    private static boolean isLate(ExamAttempt attempt, LocalDateTime now) {
        return now.isAfter(attempt.getDeadlineAt().plus(SUBMIT_GRACE));
    }

    /**
     * Score = correct / questions x 10, against the questions as they are now. Passing compares the exact fraction
     * (correct x 10 >= pass score x questions), not the rounded score, like the admin page's "at least N correct".
     */
    private void grade(Taker taker, ExamAttempt attempt, Map<Long, ExamOption> selected, LocalDateTime now) {
        List<ExamQuestion> questions = taker.exam.getQuestions();
        int correct = (int) questions.stream()
                .filter(question -> question.getCorrectOption() == selected.get(question.getId()))
                .count();
        int total = questions.size();
        BigDecimal points = BigDecimal.valueOf(correct).multiply(MAX_SCORE);
        boolean passed = total > 0
                && points.compareTo(attempt.getPassScore().multiply(BigDecimal.valueOf(total))) >= 0;

        attempt.setSubmittedAt(isLate(attempt, now) ? attempt.getDeadlineAt() : now);
        attempt.setQuestionCount(total);
        attempt.setCorrectCount(correct);
        attempt.setScore(total == 0 ? BigDecimal.ZERO.setScale(2)
                : points.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        attempt.setPassed(passed);

        CourseEnrollment enrollment = taker.enrollment;
        if (passed && enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollment.setCompletedAt(now);
        }
        attemptRepository.flush();
        log.info("Graded attempt id={}: {}/{} correct, score {}, {}", attempt.getId(), correct, total,
                attempt.getScore(), passed ? "passed" : "failed");
    }

    private MyExamResponse response(Exam exam, ExamAttempt attempt, LocalDateTime now) {
        MyExamResponse response = examMapper.toMyExam(exam);
        if (attempt == null) {
            return response;
        }
        ExamAttemptResponse attemptResponse = examMapper.toResponse(attempt);
        if (!attempt.isSubmitted()) {
            attemptResponse.setRemainingSeconds(Math.max(0, Duration.between(now, attempt.getDeadlineAt())
                    .getSeconds()));
            attemptResponse.setQuestions(exam.getQuestions().stream()
                    .map(examMapper::toPaperQuestion)
                    .collect(Collectors.toList()));
        }
        response.setAttempt(attemptResponse);
        return response;
    }

    private static final class Taker {
        private final User user;
        private final CourseEnrollment enrollment;
        private final Exam exam;

        private Taker(User user, CourseEnrollment enrollment, Exam exam) {
            this.user = user;
            this.enrollment = enrollment;
            this.exam = exam;
        }
    }
}
