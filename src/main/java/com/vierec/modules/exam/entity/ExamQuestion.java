package com.vierec.modules.exam.entity;

import com.vierec.domain.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/** Multiple-choice question of an exam ({@code exam_questions}): four options, exactly one correct. */
@Entity
@Table(name = "exam_questions",
        indexes = @Index(name = "idx_exam_questions_exam_order", columnList = "exam_id, sort_order"))
@Getter
@Setter
@NoArgsConstructor
public class ExamQuestion extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "option_a", nullable = false, length = 1000)
    private String optionA;

    @Column(name = "option_b", nullable = false, length = 1000)
    private String optionB;

    @Column(name = "option_c", nullable = false, length = 1000)
    private String optionC;

    @Column(name = "option_d", nullable = false, length = 1000)
    private String optionD;

    @Enumerated(EnumType.STRING)
    @Column(name = "correct_option", nullable = false, columnDefinition = "enum('A','B','C','D')")
    private ExamOption correctOption;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
