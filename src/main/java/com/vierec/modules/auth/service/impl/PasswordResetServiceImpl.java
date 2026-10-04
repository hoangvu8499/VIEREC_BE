package com.vierec.modules.auth.service.impl;

import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.infrastructure.mail.EmailService;
import com.vierec.modules.auth.entity.PasswordResetCode;
import com.vierec.modules.auth.repository.PasswordResetCodeRepository;
import com.vierec.modules.auth.service.PasswordResetService;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.entity.UserStatus;
import com.vierec.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_BOUND = 1_000_000;

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository codeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    @Transactional
    public void sendCode(String email) {
        User user = userRepository.findByEmail(normalize(email)).orElse(null);
        if (user == null) {
            log.info("Password reset asked for an unknown email");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (codeRepository.existsByUserIdAndCreatedAtAfter(user.getId(), now.minusSeconds(RESEND_AFTER_SECONDS))) {
            log.info("Password reset code for user id={} asked again too soon; not sent", user.getId());
            return;
        }
        codeRepository.closeOpenCodes(user.getId(), now);
        String code = String.format("%06d", RANDOM.nextInt(CODE_BOUND));
        PasswordResetCode row = new PasswordResetCode();
        row.setUser(user);
        row.setCodeHash(sha256(code));
        row.setExpiresAt(now.plusMinutes(CODE_VALID_MINUTES));
        codeRepository.save(row);
        String to = user.getEmail();
        String text = message(user, code);
        Long userId = user.getId();
        // After the commit: the SMTP round trip must not hold a database connection.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (emailService.send(to, "Mã đặt lại mật khẩu VIEREC Academy", text)) {
                    log.info("Password reset code sent to user id={}", userId);
                }
            }
        });
    }

    /** {@code noRollbackFor}: a wrong code must still count as an attempt. */
    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESET_CODE_INVALID));
        PasswordResetCode row = codeRepository.findOpenForUpdate(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESET_CODE_INVALID));
        LocalDateTime now = LocalDateTime.now();
        if (row.getExpiresAt().isBefore(now)) {
            row.setUsedAt(now);
            throw new BusinessException(ErrorCode.RESET_CODE_INVALID);
        }
        boolean matches = MessageDigest.isEqual(row.getCodeHash().getBytes(StandardCharsets.US_ASCII),
                sha256(code.trim()).getBytes(StandardCharsets.US_ASCII));
        if (!matches) {
            row.setAttempts(row.getAttempts() + 1);
            if (row.getAttempts() >= MAX_ATTEMPTS) {
                row.setUsedAt(now);
            }
            throw new BusinessException(ErrorCode.RESET_CODE_INVALID);
        }
        row.setUsedAt(now);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        if (user.getStatus() == UserStatus.ACTIVE) {
            user.setFailedLoginCount(0);
            user.setLastFailedLoginAt(null);
        }
        log.info("User id={} reset their password with an emailed code", user.getId());
    }

    private static String message(User user, String code) {
        String name = (user.getLastName() + " " + user.getFirstName()).trim();
        return "Xin chào " + name + ",\n\n"
                + "Mã đặt lại mật khẩu của bạn là: " + code + "\n"
                + "Mã có hiệu lực trong " + CODE_VALID_MINUTES + " phút. Không chia sẻ mã này cho bất kỳ ai, "
                + "kể cả nhân viên VIEREC.\n\n"
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này: mật khẩu hiện tại vẫn giữ nguyên.\n\n"
                + "VIEREC Academy";
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
