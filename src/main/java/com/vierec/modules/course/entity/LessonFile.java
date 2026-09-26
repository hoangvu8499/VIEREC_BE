package com.vierec.modules.course.entity;

import com.vierec.modules.file.entity.StoredFile;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Row of {@code lesson_files}. Mapped as an entity (not a plain {@code @ManyToMany}) because the
 * join table carries {@code file_type} and {@code sort_order}.
 */
@Entity
@Table(name = "lesson_files", indexes = @Index(name = "idx_lesson_files_file", columnList = "file_id"))
@Getter
@Setter
@NoArgsConstructor
public class LessonFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private LessonFileId id = new LessonFileId();

    @MapsId("lessonId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @MapsId("fileId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private StoredFile file;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, columnDefinition = "enum('DOCUMENT','VIDEO')")
    private LessonFileType fileType = LessonFileType.DOCUMENT;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public LessonFile(Lesson lesson, StoredFile file, LessonFileType fileType, int sortOrder) {
        this.lesson = lesson;
        this.file = file;
        this.fileType = fileType;
        this.sortOrder = sortOrder;
    }
}
