package com.vierec.modules.user.repository;

import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
