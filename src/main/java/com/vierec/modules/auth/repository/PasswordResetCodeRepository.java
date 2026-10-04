package com.vierec.modules.auth.repository;

import com.vierec.modules.auth.entity.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    /** The user's newest code that is still usable (not used, not replaced), locked against concurrent checks. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM PasswordResetCode c WHERE c.user.id = :userId AND c.usedAt IS NULL "
            + "ORDER BY c.createdAt DESC, c.id DESC")
    Optional<PasswordResetCode> findOpenForUpdate(@Param("userId") Long userId);

    boolean existsByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    @Modifying
    @Query("UPDATE PasswordResetCode c SET c.usedAt = :now WHERE c.user.id = :userId AND c.usedAt IS NULL")
    void closeOpenCodes(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
