package com.farmer.user.config;

import com.farmer.user.entity.User;
import com.farmer.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Data loader for initializing default admin user on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_ADMIN_NAME = "System Administrator";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@farmer-assistance.in";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123456";

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initializeDefaultAdmin();
    }

    private void initializeDefaultAdmin() {
        try {
            if (userRepository.findByEmail(DEFAULT_ADMIN_EMAIL).isPresent()) {
                log.info("Default admin user already exists");
                return;
            }

            long adminCount = userRepository.countByRole(User.Role.ADMIN);
            if (adminCount > 0) {
                log.info("Admin users already exist in the system");
                return;
            }

            String farmerId = "ADMIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            User defaultAdmin = User.builder()
                    .farmerId(farmerId)
                    .name(DEFAULT_ADMIN_NAME)
                    .email(DEFAULT_ADMIN_EMAIL)
                    .password(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                    .role(User.Role.ADMIN)
                    .isActive(true)
                    .build();

            userRepository.save(defaultAdmin);
            log.info("Default admin user created successfully with email: {}", DEFAULT_ADMIN_EMAIL);
            log.info("Default admin password: {} (CHANGE THIS IN PRODUCTION)", DEFAULT_ADMIN_PASSWORD);

        } catch (Exception e) {
            log.error("Failed to initialize default admin user: {}", e.getMessage(), e);
        }
    }
}
