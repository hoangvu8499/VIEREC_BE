package com.vierec.modules.exam.repository;

import com.vierec.modules.exam.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    /** Locked so that two submits of the same attempt (double click, auto-submit) are graded once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ExamAttempt a WHERE a.exam.id = :examId AND a.user.id = :userId")
    Optional<ExamAttempt> findForUpdate(@Param("examId") Long examId, @Param("userId") Long userId);

    /** Attempts of a user with their exam (and so their course id). */
    @Query("SELECT a FROM ExamAttempt a JOIN FETCH a.exam WHERE a.user.id = :userId")
    List<ExamAttempt> findAllByUserId(@Param("userId") Long userId);
}
