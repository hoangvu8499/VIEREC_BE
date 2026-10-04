package com.vierec.modules.exam.entity;

import com.vierec.domain.BaseEntity;
import com.vierec.modules.user.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A learner taking the exam of a course ({@code exam_attempts}): one row per (exam, user), as an exam can be taken
 * only once. The result columns stay {@code null} until the attempt is graded.
 */
@Entity
@Table(name = "exam_attempts",
        uniqueConstraints = @UniqueConstraint(name = "uk_exam_attempts_exam_user",
                columnNames = {"exam_id", "user_id"}),
        indexes = @Index(name = "idx_exam_attempts_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class ExamAttempt extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /** Start + exam duration at start time: changing the duration later does not move it. */
    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    /** Pass score at start time. */
    @Column(name = "pass_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal passScore;

    /** {@code null} while the learner is answering. */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "question_count")
    private Integer questionCount;

    @Column(name = "correct_count")
    private Integer correctCount;

    /** Out of 10. */
    @Column(name = "score", precision = 4, scale = 2)
    private BigDecimal score;

    @Column(name = "passed")
    private Boolean passed;

    public boolean isSubmitted() {
        return submittedAt != null;
    }
}
