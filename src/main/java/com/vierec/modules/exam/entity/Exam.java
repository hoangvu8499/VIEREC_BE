package com.vierec.modules.exam.entity;

import com.vierec.domain.BaseEntity;
import com.vierec.modules.course.entity.Course;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Certificate exam of a course ({@code exams}): at most one per course. The score is out of 10:
 * correct answers / number of questions x 10.
 */
@Entity
@Table(name = "exams", uniqueConstraints = @UniqueConstraint(name = "uk_exams_course", columnNames = "course_id"))
@Getter
@Setter
@NoArgsConstructor
public class Exam extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    /** Minimum score (out of 10) to pass. */
    @Column(name = "pass_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal passScore;

    /** Removing a question from this list deletes its row. */
    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<ExamQuestion> questions = new ArrayList<>();
}
