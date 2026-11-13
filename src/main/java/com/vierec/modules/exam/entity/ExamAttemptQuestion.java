package com.vierec.modules.exam.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;

/**
 * A question drawn for an attempt ({@code exam_attempt_questions}): the paper of a learner is a random subset of the
 * exam's questions, in a random order, and is graded on exactly these questions. Deleting the question (or the
 * attempt) deletes the row in the database.
 */
@Entity
@Table(name = "exam_attempt_questions",
        uniqueConstraints = @UniqueConstraint(name = "uk_exam_attempt_questions",
                columnNames = {"attempt_id", "question_id"}),
        indexes = @Index(name = "idx_exam_attempt_questions_question", columnList = "question_id"))
@Getter
@Setter
@NoArgsConstructor
public class ExamAttemptQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ExamQuestion question;

    /** Position on the learner's paper, from 1. */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
