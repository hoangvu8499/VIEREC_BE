package com.vierec.modules.auth.service.impl;

import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.modules.auth.service.LoginAttemptService;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.entity.UserStatus;
import com.vierec.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Writes in their own transactions: a failed login ends with an exception, which must not undo the count. */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public void checkNotBlocked(String usernameOrEmail) {
        userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .filter(user -> user.hasRole(RoleCode.SUPER_ADMIN) && blockedUntil(user).isAfter(LocalDateTime.now()))
                .ifPresent(user -> {
                    throw new BusinessException(ErrorCode.LOGIN_TEMPORARILY_BLOCKED);
                });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordFailure(String usernameOrEmail) {
        return userRepository.findForUpdateByUsernameOrEmail(usernameOrEmail)
                .map(this::countFailure)
                .orElse(false);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(Long userId) {
        userRepository.resetFailedLogins(userId);
    }

    private boolean countFailure(User user) {
        user.setFailedLoginCount(user.getFailedLoginCount() + 1);
        user.setLastFailedLoginAt(LocalDateTime.now());
        boolean lock = user.getFailedLoginCount() >= MAX_FAILURES && user.getStatus() == UserStatus.ACTIVE
                && !user.hasRole(RoleCode.SUPER_ADMIN);
        if (lock) {
            user.setStatus(UserStatus.LOCKED);
            log.warn("User id={} locked after {} wrong passwords", user.getId(), user.getFailedLoginCount());
        }
        return lock;
    }

    private static LocalDateTime blockedUntil(User user) {
        if (user.getFailedLoginCount() < MAX_FAILURES || user.getLastFailedLoginAt() == null) {
            return LocalDateTime.MIN;
        }
        return user.getLastFailedLoginAt().plusMinutes(SUPER_ADMIN_BLOCK_MINUTES);
    }
}
