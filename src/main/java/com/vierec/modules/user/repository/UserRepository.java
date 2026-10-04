package com.vierec.modules.user.repository;

import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * JPQL queries here only see live rows ({@code @Where(deleted_at IS NULL)} on {@link User}).
 * The {@code count...IncludingDeleted} native queries also see soft-deleted rows: a deleted user still holds
 * its unique values in the table, so uniqueness must be checked against every row.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.username = :name OR u.email = :name")
    Optional<User> findForUpdateByUsernameOrEmail(@Param("name") String usernameOrEmail);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginCount = 0, u.lastFailedLoginAt = NULL "
            + "WHERE u.id = :id AND u.failedLoginCount > 0")
    void resetFailedLogins(@Param("id") Long id);

    boolean existsByUsername(String username);

    @Query(value = "SELECT COUNT(*) FROM users WHERE username = :username", nativeQuery = true)
    long countByUsernameIncludingDeleted(@Param("username") String username);

    @Query(value = "SELECT COUNT(*) FROM users WHERE email = :email", nativeQuery = true)
    long countByEmailIncludingDeleted(@Param("email") String email);

    @Query(value = "SELECT COUNT(*) FROM users WHERE phone_number = :phone", nativeQuery = true)
    long countByPhoneNumberIncludingDeleted(@Param("phone") String phoneNumber);

    @Query(value = "SELECT COUNT(*) FROM users WHERE cccd = :cccd", nativeQuery = true)
    long countByCccdIncludingDeleted(@Param("cccd") String cccd);

    @Query("SELECT u FROM User u WHERE "
            + "(:keyword IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
            + "AND (:status IS NULL OR u.status = :status)")
    Page<User> search(@Param("keyword") String keyword,
                      @Param("status") UserStatus status,
                      Pageable pageable);

    /** WHERE clause matching the managers (role BUSINESS) of a business; alias {@code u}. */
    String IS_MANAGER = "EXISTS (SELECT ur FROM UserRole ur WHERE ur.user = u AND ur.role.code = 'BUSINESS')";

    /** Learners of a business (its users without the BUSINESS role), keyword on username, name, phone, email. */
    @Query("SELECT u FROM User u WHERE u.business.id = :businessId AND NOT " + IS_MANAGER + " AND "
            + "(:keyword IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(CONCAT(u.lastName, ' ', u.firstName)) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR u.phoneNumber LIKE CONCAT('%', :keyword, '%') "
            + "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> findMembers(@Param("businessId") Long businessId,
                           @Param("keyword") String keyword,
                           Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.business.id = :businessId AND " + IS_MANAGER + " ORDER BY u.id")
    List<User> findManagers(@Param("businessId") Long businessId);

    @Query("SELECT u FROM User u WHERE u.business.id = :businessId AND u.id IN :ids AND NOT " + IS_MANAGER)
    List<User> findMembersByIds(@Param("businessId") Long businessId, @Param("ids") Collection<Long> ids);

    /** Rows of [business id, learner count]. */
    @Query("SELECT u.business.id, COUNT(u) FROM User u WHERE u.business.id IN :businessIds AND NOT " + IS_MANAGER
            + " GROUP BY u.business.id")
    List<Object[]> countMembers(@Param("businessIds") Collection<Long> businessIds);

    /** Rows of [business id, manager count]. */
    @Query("SELECT u.business.id, COUNT(u) FROM User u WHERE u.business.id IN :businessIds AND " + IS_MANAGER
            + " GROUP BY u.business.id")
    List<Object[]> countManagers(@Param("businessIds") Collection<Long> businessIds);
}
