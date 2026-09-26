package com.vierec.modules.course.entity;

import com.vierec.domain.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lesson row from {@code lessons}: one course has many lessons, ordered by {@link #sortOrder}.
 */
@Entity
@Table(name = "lessons", indexes = @Index(name = "idx_lessons_course_order", columnList = "course_id, sort_order"))
@Where(clause = "deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Lesson extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "document_url", length = 2048)
    private String documentUrl;

    /** Link to a video hosted elsewhere (YouTube, Drive...); independent of an uploaded video file. */
    @Column(name = "video_url", length = 2048)
    private String videoUrl;

    /** TEXT column, see {@link Course#getDescription()}. */
    @Column(name = "instructions", columnDefinition = "text")
    private String instructions;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** Attached files in display order. Removing an entry deletes the {@code lesson_files} row, not the file. */
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<LessonFile> lessonFiles = new ArrayList<>();

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
