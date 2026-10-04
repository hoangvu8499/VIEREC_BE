package com.vierec.modules.exam.dto;

public enum ExamAttemptStatus {

    /** The learner is answering; {@code remainingSeconds} and {@code questions} are set. */
    IN_PROGRESS,
    /** Graded; {@code correctCount}, {@code score} and {@code passed} are set. */
    SUBMITTED
}
