package com.vierec.modules.course.entity;

import com.vierec.modules.user.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
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
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Row of {@code course_enrollments}: a user taking a course. At most one row per (user, course).
 *
 * <p>Does not extend {@code BaseEntity}: the "created" column is {@code enrolled_at}, not {@code created_at}.</p>
 */
@Entity
@Table(name = "course_enrollments",
        uniqueConstraints = @UniqueConstraint(name = "uk_enrollments_user_course",
                columnNames = {"user_id", "course_id"}),
        indexes = @Index(name = "idx_enrollments_course", columnList = "course_id"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class CourseEnrollment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "enum('PENDING','ENROLLED','COMPLETED','CANCELLED')")
    private EnrollmentStatus status = EnrollmentStatus.PENDING;

    /**
     * Course price when the learner asked to join, i.e. the amount they were told to transfer. Kept so that
     * revenue does not change when the course price does.
     */
    @Column(name = "price", nullable = false)
    private Long price = 0L;

    /** When an admin let the learner in (PENDING → ENROLLED); revenue is counted by this date. */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** Reset by the service when a cancelled enrollment is taken up again. */
    @CreatedDate
    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    /** Set by the service when {@link #status} becomes {@link EnrollmentStatus#COMPLETED}. */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
