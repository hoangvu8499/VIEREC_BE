package com.vierec.modules.course.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Composite key (lesson_id, file_id) of {@code lesson_files}.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LessonFileId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "file_id")
    private Long fileId;
}
