package com.vierec.modules.exam.service;

import com.vierec.modules.exam.dto.ExamImportMode;
import com.vierec.modules.exam.dto.ExamImportResponse;
import com.vierec.modules.exam.dto.ExamQuestionRequest;
import com.vierec.modules.exam.dto.ExamQuestionResponse;
import com.vierec.modules.exam.dto.ExamRequest;
import com.vierec.modules.exam.dto.ExamResponse;
import org.springframework.web.multipart.MultipartFile;

/** Certificate exam of a course and its questions (admin side). */
public interface ExamService {

    ExamResponse get(Long courseId);

    /** Creates the exam of the course, or replaces the settings of the existing one. */
    ExamResponse save(Long courseId, ExamRequest request);

    /** Adds the question after the last one. */
    ExamQuestionResponse addQuestion(Long courseId, ExamQuestionRequest request);

    ExamQuestionResponse updateQuestion(Long courseId, Long questionId, ExamQuestionRequest request);

    void deleteQuestion(Long courseId, Long questionId);

    /** All or nothing: a file with an invalid row changes nothing. */
    ExamImportResponse importQuestions(Long courseId, MultipartFile file, ExamImportMode mode);
}
