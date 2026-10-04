package com.vierec.modules.exam.mapper;

import com.vierec.modules.exam.dto.ExamAttemptResponse;
import com.vierec.modules.exam.dto.ExamAttemptStatus;
import com.vierec.modules.exam.dto.ExamPaperQuestionResponse;
import com.vierec.modules.exam.dto.ExamQuestionResponse;
import com.vierec.modules.exam.dto.ExamResponse;
import com.vierec.modules.exam.dto.MyExamResponse;
import com.vierec.modules.exam.entity.Exam;
import com.vierec.modules.exam.entity.ExamAttempt;
import com.vierec.modules.exam.entity.ExamQuestion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/** Exam entity to DTO conversions. Requests are not mapped here: the service sets every entity field. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = ExamAttemptStatus.class)
public interface ExamMapper {

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "questionCount", expression = "java(exam.getQuestions().size())")
    ExamResponse toResponse(Exam exam);

    ExamQuestionResponse toResponse(ExamQuestion question);

    /** Without the attempt: the service adds it. */
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "questionCount", expression = "java(exam.getQuestions().size())")
    @Mapping(target = "attempt", ignore = true)
    MyExamResponse toMyExam(Exam exam);

    /** Without the questions and the remaining time: the service adds them while the attempt is in progress. */
    @Mapping(target = "status", expression = "java(attempt.isSubmitted() ? ExamAttemptStatus.SUBMITTED "
            + ": ExamAttemptStatus.IN_PROGRESS)")
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "remainingSeconds", ignore = true)
    ExamAttemptResponse toResponse(ExamAttempt attempt);

    /** The question without its correct answer. */
    ExamPaperQuestionResponse toPaperQuestion(ExamQuestion question);
}
