package com.vierec.modules.course.repository;

import com.vierec.modules.course.entity.Course;
import com.vierec.modules.course.entity.CourseStatus;
import com.vierec.modules.course.entity.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

/**
 * JPQL queries here only see live courses ({@code @Where(deleted_at IS NULL)} on {@link Course}).
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * WHERE clause of {@link #search}: {@code excludeUsername} null keeps every course, otherwise the courses that
     * user has an enrollment with one of {@code excludeStatuses} are left out.
     */
    String SEARCH_FILTER = "(:status IS NULL OR c.status = :status) "
            + "AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
            + "AND (:excludeUsername IS NULL OR NOT EXISTS (SELECT e.id FROM CourseEnrollment e "
            + "WHERE e.course = c AND e.user.username = :excludeUsername AND e.status IN :excludeStatuses))";

    /** Instructor and creator are fetched in the same query so a page of rows needs no extra selects. */
    @Query(value = "SELECT c FROM Course c LEFT JOIN FETCH c.instructor LEFT JOIN FETCH c.createdBy WHERE "
            + SEARCH_FILTER,
            countQuery = "SELECT COUNT(c) FROM Course c WHERE " + SEARCH_FILTER)
    Page<Course> search(@Param("keyword") String keyword,
                        @Param("status") CourseStatus status,
                        @Param("excludeUsername") String excludeUsername,
                        @Param("excludeStatuses") Collection<EnrollmentStatus> excludeStatuses,
                        Pageable pageable);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.instructor LEFT JOIN FETCH c.createdBy WHERE c.id = :id")
    Optional<Course> findDetailById(@Param("id") Long id);
}
