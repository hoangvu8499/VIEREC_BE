package com.vierec.modules.exam.service;

import com.vierec.modules.exam.dto.ExamAnswerRequest;
import com.vierec.modules.exam.dto.MyExamResponse;

import java.util.List;

/**
 * A learner taking the certificate exam of a course (learner side). Only approved learners of the course
 * (ENROLLED or COMPLETED) may take it, once. Grading is done here: the correct answers never leave the server.
 */
public interface ExamAttemptService {

    /** The exam rules and the learner's attempt, if any. An attempt whose time ran out is graded first. */
    MyExamResponse get(Long courseId, String username);

    /** Starts the attempt and returns its questions; returns the current attempt when there is one already. */
    MyExamResponse start(Long courseId, String username);

    /**
     * Grades the attempt. Answers that arrive after the deadline (plus a short grace for the network) are not
     * counted. Passing completes the enrollment.
     */
    MyExamResponse submit(Long courseId, String username, List<ExamAnswerRequest> answers);
}
