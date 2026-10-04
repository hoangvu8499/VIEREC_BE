package com.vierec.modules.exam.service.impl;

import com.vierec.common.exception.ErrorCode;
import com.vierec.common.exception.ResourceNotFoundException;
import com.vierec.modules.course.entity.Course;
import com.vierec.modules.course.repository.CourseRepository;
import com.vierec.modules.exam.dto.ExamImportMode;
import com.vierec.modules.exam.dto.ExamImportResponse;
import com.vierec.modules.exam.dto.ExamQuestionRequest;
import com.vierec.modules.exam.dto.ExamQuestionResponse;
import com.vierec.modules.exam.dto.ExamRequest;
import com.vierec.modules.exam.dto.ExamResponse;
import com.vierec.modules.exam.entity.Exam;
import com.vierec.modules.exam.entity.ExamQuestion;
import com.vierec.modules.exam.mapper.ExamMapper;
import com.vierec.modules.exam.repository.ExamRepository;
import com.vierec.modules.exam.service.ExamQuestionWorkbook;
import com.vierec.modules.exam.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamServiceImpl implements ExamService {

    private final CourseRepository courseRepository;
    private final ExamRepository examRepository;
    private final ExamQuestionWorkbook workbook;
    private final ExamMapper examMapper;

    @Override
    public ExamResponse get(Long courseId) {
        return examMapper.toResponse(findExam(courseId));
    }

    @Override
    @Transactional
    public ExamResponse save(Long courseId, ExamRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND));
        Exam exam = examRepository.findWithQuestionsByCourseId(courseId).orElse(null);
        boolean created = exam == null;
        if (created) {
            exam = new Exam();
            exam.setCourse(course);
        }
        exam.setTitle(request.getTitle().trim());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setPassScore(request.getPassScore());

        Exam saved = examRepository.saveAndFlush(exam);
        log.info("{} exam id={} of course id={}", created ? "Created" : "Updated", saved.getId(), courseId);
        return examMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ExamQuestionResponse addQuestion(Long courseId, ExamQuestionRequest request) {
        Exam exam = findExam(courseId);
        ExamQuestion question = append(exam, request);
        // The exam is managed: flushing persists the new question into this very instance (save() would merge
        // a copy and leave it without an id).
        examRepository.flush();
        log.info("Added question id={} to exam id={}", question.getId(), exam.getId());
        return examMapper.toResponse(question);
    }

    @Override
    @Transactional
    public ExamQuestionResponse updateQuestion(Long courseId, Long questionId, ExamQuestionRequest request) {
        Exam exam = findExam(courseId);
        ExamQuestion question = findQuestion(exam, questionId);
        apply(question, request);
        examRepository.flush();
        log.info("Updated question id={} of exam id={}", questionId, exam.getId());
        return examMapper.toResponse(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long courseId, Long questionId) {
        Exam exam = findExam(courseId);
        exam.getQuestions().remove(findQuestion(exam, questionId));
        examRepository.flush();
        log.info("Deleted question id={} of exam id={}", questionId, exam.getId());
    }

    @Override
    @Transactional
    public ExamImportResponse importQuestions(Long courseId, MultipartFile file, ExamImportMode mode) {
        Exam exam = findExam(courseId);
        // Read (and reject) the whole file before touching the current questions.
        List<ExamQuestionRequest> rows = workbook.read(file);
        if (mode == ExamImportMode.REPLACE) {
            exam.getQuestions().clear();
        }
        rows.forEach(row -> append(exam, row));

        examRepository.flush();
        log.info("Imported {} questions into exam id={} ({}), {} in total", rows.size(), exam.getId(), mode,
                exam.getQuestions().size());
        return ExamImportResponse.builder()
                .importedCount(rows.size())
                .exam(examMapper.toResponse(exam))
                .build();
    }

    /** The course is checked first so that the exam of a soft-deleted course is not found either. */
    private Exam findExam(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND);
        }
        return examRepository.findWithQuestionsByCourseId(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EXAM_NOT_FOUND));
    }

    private static ExamQuestion findQuestion(Exam exam, Long questionId) {
        return exam.getQuestions().stream()
                .filter(question -> question.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EXAM_QUESTION_NOT_FOUND));
    }

    private static ExamQuestion append(Exam exam, ExamQuestionRequest request) {
        int last = exam.getQuestions().stream().mapToInt(ExamQuestion::getSortOrder).max().orElse(0);
        ExamQuestion question = new ExamQuestion();
        question.setExam(exam);
        question.setSortOrder(last + 1);
        apply(question, request);
        exam.getQuestions().add(question);
        return question;
    }

    private static void apply(ExamQuestion question, ExamQuestionRequest request) {
        question.setContent(request.getContent().trim());
        question.setOptionA(request.getOptionA().trim());
        question.setOptionB(request.getOptionB().trim());
        question.setOptionC(request.getOptionC().trim());
        question.setOptionD(request.getOptionD().trim());
        question.setCorrectOption(request.getCorrectOption());
    }
}
