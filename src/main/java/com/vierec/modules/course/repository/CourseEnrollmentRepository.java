package com.vierec.modules.course.repository;

import com.vierec.modules.course.entity.CourseEnrollment;
import com.vierec.modules.course.entity.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The {@code @Where} of {@code Course} and {@code User} is not applied to joined entities, so the queries below
 * filter soft-deleted courses and users themselves.
 */
@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    /** Aliases {@code e} (enrollment), {@code u} (user), {@code c} (course); a null keyword matches everything. */
    String KEYWORD_FILTER = "(:keyword IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(CONCAT(u.lastName, ' ', u.firstName)) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR u.phoneNumber LIKE CONCAT('%', :keyword, '%') "
            + "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))";

    /** WHERE clause of {@link #search}. */
    String SEARCH_FILTER = "(:status IS NULL OR e.status = :status) AND u.deletedAt IS NULL AND c.deletedAt IS NULL "
            + "AND " + KEYWORD_FILTER;

    /**
     * WHERE clause of the revenue queries: approved in [from, to) and still learning. Deleted courses and users
     * still count: the money was received.
     */
    String APPROVED_FILTER = "e.status IN :statuses AND e.approvedAt >= :from AND e.approvedAt < :to AND "
            + KEYWORD_FILTER;

    Optional<CourseEnrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    Optional<CourseEnrollment> findByIdAndCourseId(Long id, Long courseId);

    List<CourseEnrollment> findByCourseIdAndUserIdIn(Long courseId, Collection<Long> userIds);

    boolean existsByUserUsernameAndCourseIdInAndStatusIn(String username, Collection<Long> courseIds,
                                                         Collection<EnrollmentStatus> statuses);

    @Query(value = "SELECT e FROM CourseEnrollment e JOIN FETCH e.course c LEFT JOIN FETCH c.instructor "
            + "WHERE e.user.id = :userId AND e.status IN :statuses AND c.deletedAt IS NULL",
            countQuery = "SELECT COUNT(e) FROM CourseEnrollment e JOIN e.course c "
                    + "WHERE e.user.id = :userId AND e.status IN :statuses AND c.deletedAt IS NULL")
    Page<CourseEnrollment> findByUser(@Param("userId") Long userId,
                                      @Param("statuses") Collection<EnrollmentStatus> statuses,
                                      Pageable pageable);

    @Query(value = "SELECT e FROM CourseEnrollment e JOIN FETCH e.user u JOIN FETCH e.course "
            + "WHERE e.course.id = :courseId AND (:status IS NULL OR e.status = :status) AND u.deletedAt IS NULL",
            countQuery = "SELECT COUNT(e) FROM CourseEnrollment e JOIN e.user u "
                    + "WHERE e.course.id = :courseId AND (:status IS NULL OR e.status = :status) "
                    + "AND u.deletedAt IS NULL")
    Page<CourseEnrollment> findByCourse(@Param("courseId") Long courseId,
                                        @Param("status") EnrollmentStatus status,
                                        Pageable pageable);

    /** Every course, for the admin approval queue: live users and courses only. */
    @Query(value = "SELECT e FROM CourseEnrollment e JOIN FETCH e.user u JOIN FETCH e.course c "
            + "LEFT JOIN FETCH c.instructor WHERE " + SEARCH_FILTER,
            countQuery = "SELECT COUNT(e) FROM CourseEnrollment e JOIN e.user u JOIN e.course c WHERE "
                    + SEARCH_FILTER)
    Page<CourseEnrollment> search(@Param("status") EnrollmentStatus status,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);

    /**
     * One row of [sum of prices, count] of the enrollments approved since {@code from} and still learning.
     * Deleted courses and users still count: the money was received.
     */
    @Query("SELECT COALESCE(SUM(e.price), 0), COUNT(e) FROM CourseEnrollment e "
            + "WHERE e.status IN :statuses AND e.approvedAt >= :from")
    List<Object[]> sumApprovedSince(@Param("statuses") Collection<EnrollmentStatus> statuses,
                                    @Param("from") LocalDateTime from);

    /** Approved enrollments in [from, to) with the payer and course, for the monthly revenue details. */
    @Query(value = "SELECT e FROM CourseEnrollment e JOIN FETCH e.user u JOIN FETCH e.course c "
            + "LEFT JOIN FETCH c.instructor WHERE " + APPROVED_FILTER,
            countQuery = "SELECT COUNT(e) FROM CourseEnrollment e JOIN e.user u JOIN e.course c WHERE "
                    + APPROVED_FILTER)
    Page<CourseEnrollment> findApproved(@Param("statuses") Collection<EnrollmentStatus> statuses,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to,
                                        @Param("keyword") String keyword,
                                        Pageable pageable);

    /** One row of [sum of prices, count] of {@link #findApproved}. */
    @Query("SELECT COALESCE(SUM(e.price), 0), COUNT(e) FROM CourseEnrollment e JOIN e.user u JOIN e.course c "
            + "WHERE " + APPROVED_FILTER)
    List<Object[]> sumApproved(@Param("statuses") Collection<EnrollmentStatus> statuses,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to,
                               @Param("keyword") String keyword);

    /** Rows of [course id, course name, sum of prices, count] approved in [from, to), highest sum first. */
    @Query("SELECT c.id, c.name, SUM(e.price), COUNT(e) FROM CourseEnrollment e JOIN e.course c "
            + "WHERE e.status IN :statuses AND e.approvedAt >= :from AND e.approvedAt < :to "
            + "GROUP BY c.id, c.name ORDER BY SUM(e.price) DESC, c.id")
    List<Object[]> sumApprovedByCourse(@Param("statuses") Collection<EnrollmentStatus> statuses,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    /** Rows of [course id, status] for the given user and courses. */
    @Query("SELECT e.course.id, e.status FROM CourseEnrollment e "
            + "WHERE e.user.username = :username AND e.course.id IN :courseIds")
    List<Object[]> findStatuses(@Param("username") String username,
                                @Param("courseIds") Collection<Long> courseIds);

    /** Every enrollment of a user (any status) with its course, live courses only, newest first. */
    @Query("SELECT e FROM CourseEnrollment e JOIN FETCH e.course c WHERE e.user.id = :userId AND c.deletedAt IS NULL "
            + "ORDER BY e.enrolledAt DESC, e.id DESC")
    List<CourseEnrollment> findAllByUserWithCourse(@Param("userId") Long userId);

    /** Rows of [user id, status, count] for the given users, live courses only. */
    @Query("SELECT e.user.id, e.status, COUNT(e) FROM CourseEnrollment e JOIN e.course c "
            + "WHERE e.user.id IN :userIds AND c.deletedAt IS NULL GROUP BY e.user.id, e.status")
    List<Object[]> countByUsersAndStatus(@Param("userIds") Collection<Long> userIds);
}
