package com.vierec.modules.course.entity;

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

/**
 * How much of one video of a lesson a learner watched ({@code lesson_progress}). The video is identified by
 * {@link CourseVideos} keys: {@code youtube-<id>} for the lesson's YouTube link, {@code file-<id>} for an uploaded
 * VIDEO file.
 */
@Entity
@Table(name = "lesson_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_lesson_progress_video",
                columnNames = {"user_id", "lesson_id", "video_key"}),
        indexes = @Index(name = "idx_lesson_progress_lesson", columnList = "lesson_id"))
@Getter
@Setter
@NoArgsConstructor
public class LessonProgress extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "video_key", nullable = false, length = 100)
    private String videoKey;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds = 0;

    /** Distinct seconds watched (watching a part again does not count twice), at most the duration. */
    @Column(name = "watched_seconds", nullable = false)
    private Integer watchedSeconds = 0;
}
