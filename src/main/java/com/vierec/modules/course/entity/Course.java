package com.vierec.modules.course.entity;

import com.vierec.domain.BaseEntity;
import com.vierec.modules.user.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Course row from {@code courses}. Soft-deleted rows ({@code deleted_at} set) are hidden from JPA reads.
 */
@Entity
@Table(name = "courses")
@Where(clause = "deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Course extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public static final long DEFAULT_PRICE = 100_000L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** TEXT column: the definition keeps {@code ddl-auto: validate} from expecting a VARCHAR. */
    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** Instructor is an ordinary user; {@code ON DELETE SET NULL} in the DB. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id")
    private User instructor;

    /** Học phí (VND). Rows created before prices existed got the column default (100,000). */
    @Column(name = "price", nullable = false)
    private Long price = DEFAULT_PRICE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "enum('DRAFT','PUBLISHED','ARCHIVED')")
    private CourseStatus status = CourseStatus.DRAFT;

    /** User who created the course; set by the service, not by Spring auditing (no AuditorAware). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** Live lessons only (the {@code @Where} of {@link Lesson} also applies here), in course order. */
    @OneToMany(mappedBy = "course")
    @OrderBy("sortOrder ASC, id ASC")
    private List<Lesson> lessons = new ArrayList<>();

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
