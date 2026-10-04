package com.vierec.modules.course.repository;

import com.vierec.modules.course.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * JPQL queries here only see live lessons ({@code @Where(deleted_at IS NULL)} on {@link Lesson}).
 */
@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    boolean existsByCourseIdAndSortOrder(Long courseId, Integer sortOrder);

    boolean existsByCourseIdAndSortOrderAndIdNot(Long courseId, Integer sortOrder, Long id);

    Optional<Lesson> findByIdAndCourseId(Long id, Long courseId);

    /** Lessons of one course with their files, in course order, in a single query. */
    @Query("SELECT DISTINCT l FROM Lesson l LEFT JOIN FETCH l.lessonFiles lf LEFT JOIN FETCH lf.file "
            + "WHERE l.course.id = :courseId ORDER BY l.sortOrder ASC, l.id ASC")
    List<Lesson> findAllWithFilesByCourseId(@Param("courseId") Long courseId);

    /** {@link #findAllWithFilesByCourseId} for several courses. */
    @Query("SELECT DISTINCT l FROM Lesson l LEFT JOIN FETCH l.lessonFiles lf LEFT JOIN FETCH lf.file "
            + "WHERE l.course.id IN :courseIds ORDER BY l.sortOrder ASC, l.id ASC")
    List<Lesson> findAllWithFilesByCourseIds(@Param("courseIds") Collection<Long> courseIds);

    /** Courses whose lessons (deleted ones included) use file {@code fileId}; empty for files of other modules. */
    @Query("SELECT DISTINCT lf.lesson.course.id FROM LessonFile lf WHERE lf.file.id = :fileId")
    List<Long> findCourseIdsByFileId(@Param("fileId") Long fileId);

    /** Rows of {@code [courseId, lessonCount]}; courses without lessons are absent. */
    @Query("SELECT l.course.id, COUNT(l) FROM Lesson l WHERE l.course.id IN :courseIds GROUP BY l.course.id")
    List<Object[]> countByCourseIds(@Param("courseIds") Collection<Long> courseIds);
}
