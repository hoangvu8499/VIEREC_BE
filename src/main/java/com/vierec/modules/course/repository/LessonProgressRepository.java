package com.vierec.modules.course.repository;

import com.vierec.modules.course.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    /** Every progress row of the user in the course (rows of deleted lessons included: callers filter). */
    @Query("SELECT p FROM LessonProgress p WHERE p.user.id = :userId AND p.lesson.course.id = :courseId")
    List<LessonProgress> findByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("SELECT p FROM LessonProgress p WHERE p.user.id = :userId AND p.lesson.course.id IN :courseIds")
    List<LessonProgress> findByUserAndCourses(@Param("userId") Long userId,
                                              @Param("courseIds") Collection<Long> courseIds);
}
