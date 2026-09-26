package com.vierec.infrastructure;

import com.vierec.modules.role.entity.Role;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.role.repository.RoleRepository;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.entity.UserStatus;
import com.vierec.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a first SUPER_ADMIN account so a fresh environment is usable. Only runs when
 * {@code app.seed-admin=true}; off by default because it writes to the database.
 *
 * <p>Seeding lives here rather than in SQL because the password has to be hashed by the
 * application's own {@link PasswordEncoder}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-admin", havingValue = "true")
public class DataInitializer implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin@123";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.countByUsernameIncludingDeleted(ADMIN_USERNAME) > 0) {
            return;
        }
        Role superAdmin = roleRepository.findByCode(RoleCode.SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Role SUPER_ADMIN is missing from table roles"));

        User admin = User.builder()
                .username(ADMIN_USERNAME)
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                .firstName("Administrator")
                .lastName("System")
                .status(UserStatus.ACTIVE)
                .build();
        admin.addRole(superAdmin, null);

        userRepository.save(admin);
        log.warn("Seeded default admin account '{}' with password '{}' - change it.", ADMIN_USERNAME, ADMIN_PASSWORD);
    }
}
