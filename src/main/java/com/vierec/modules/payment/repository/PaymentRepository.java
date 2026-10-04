package com.vierec.modules.payment.repository;

import com.vierec.modules.payment.entity.Payment;
import com.vierec.modules.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Payer, course and recorder are fetched in the same query. Joined entities ignore their {@code @Where}, so a
 * payment stays readable after its user or course was soft-deleted.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query(value = "SELECT p FROM Payment p JOIN FETCH p.user u JOIN FETCH p.course LEFT JOIN FETCH p.createdBy "
            + "WHERE (:userId IS NULL OR u.id = :userId) "
            + "AND (:courseId IS NULL OR p.course.id = :courseId) "
            + "AND (:status IS NULL OR p.status = :status)",
            countQuery = "SELECT COUNT(p) FROM Payment p "
                    + "WHERE (:userId IS NULL OR p.user.id = :userId) "
                    + "AND (:courseId IS NULL OR p.course.id = :courseId) "
                    + "AND (:status IS NULL OR p.status = :status)")
    Page<Payment> search(@Param("userId") Long userId,
                         @Param("courseId") Long courseId,
                         @Param("status") PaymentStatus status,
                         Pageable pageable);

    @Query("SELECT p FROM Payment p JOIN FETCH p.user JOIN FETCH p.course LEFT JOIN FETCH p.createdBy "
            + "WHERE p.id = :id")
    Optional<Payment> findDetailById(@Param("id") Long id);
}
