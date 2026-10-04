package com.vierec.modules.certificate.repository;

import com.vierec.modules.certificate.entity.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    /** Aliases {@code c} (certificate); a null keyword matches everything. */
    String KEYWORD_FILTER = "(:keyword IS NULL OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%')))";

    Optional<Certificate> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByEnrollmentId(Long enrollmentId);

    @Query("SELECT COUNT(c) > 0 FROM Certificate c WHERE c.enrollment.user.id = :userId")
    boolean existsByUserId(@Param("userId") Long userId);

    /** Rows of [owner id, owner's business id or null] of the certificate whose PDF is {@code fileId}. */
    @Query("SELECT u.id, b.id FROM Certificate c JOIN c.enrollment e JOIN e.user u LEFT JOIN u.business b "
            + "WHERE c.file.id = :fileId")
    List<Object[]> findOwnerByFileId(@Param("fileId") Long fileId);

    @Query("SELECT c FROM Certificate c JOIN FETCH c.enrollment e WHERE c.code = :code AND e.user.id = :userId")
    Optional<Certificate> findByCodeAndUserId(@Param("code") String code, @Param("userId") Long userId);

    @Query("SELECT c FROM Certificate c JOIN FETCH c.enrollment e JOIN e.user u "
            + "WHERE c.code = :code AND u.business.id = :businessId")
    Optional<Certificate> findByCodeAndBusinessId(@Param("code") String code,
                                                  @Param("businessId") Long businessId);

    /** Certificates of the learners of a business; keyword on code, learner name or course name. */
    @Query(value = "SELECT c FROM Certificate c JOIN FETCH c.enrollment e JOIN e.user u "
            + "WHERE u.business.id = :businessId AND " + KEYWORD_FILTER,
            countQuery = "SELECT COUNT(c) FROM Certificate c JOIN c.enrollment e JOIN e.user u "
                    + "WHERE u.business.id = :businessId AND " + KEYWORD_FILTER)
    Page<Certificate> findByBusinessId(@Param("businessId") Long businessId,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);

    @Query(value = "SELECT c FROM Certificate c JOIN FETCH c.enrollment e WHERE e.user.id = :userId",
            countQuery = "SELECT COUNT(c) FROM Certificate c WHERE c.enrollment.user.id = :userId")
    Page<Certificate> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /** Certificates of a user with their enrollment (to match them with the user's courses). */
    @Query("SELECT c FROM Certificate c JOIN FETCH c.enrollment e WHERE e.user.id = :userId")
    List<Certificate> findAllByUserId(@Param("userId") Long userId);

    /** Rows of [user id, certificate count]. */
    @Query("SELECT c.enrollment.user.id, COUNT(c) FROM Certificate c WHERE c.enrollment.user.id IN :userIds "
            + "GROUP BY c.enrollment.user.id")
    List<Object[]> countByUserIds(@Param("userIds") Collection<Long> userIds);
}
