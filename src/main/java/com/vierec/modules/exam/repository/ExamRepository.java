package com.vierec.modules.exam.repository;

import com.vierec.modules.exam.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    /** The exam of a course with its questions (in exam order), in a single query. */
    @Query("SELECT DISTINCT e FROM Exam e LEFT JOIN FETCH e.questions WHERE e.course.id = :courseId")
    Optional<Exam> findWithQuestionsByCourseId(@Param("courseId") Long courseId);
}
