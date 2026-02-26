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
 * Requirements: 22.1, 22.2
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_ADMIN_PHONE = "+91-9999999999";
    private static final String DEFAULT_ADMIN_NAME = "System Administrator";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@farmer-assistance.in";
    private static final String DEFAULT_ADMIN_STATE = "National";
    private static final String DEFAULT_ADMIN_DISTRICT = "Admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123456";

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initializeDefaultAdmin();
    }

    /**
     * Initialize default admin user if it doesn't exist.
     */
    private void initializeDefaultAdmin() {
        try {
            if (userRepository.findByPhone(DEFAULT_ADMIN_PHONE).isPresent()) {
                log.info("Default admin user already exists");
                return;
            }

            long adminCount = userRepository.countByRole(User.Role.ADMIN);
            if (adminCount > 0) {
                log.info("Admin users already exist in the system");
                return;
            }

            String farmerId = "ADMIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String aadhaarHash = "DEFAULT_ADMIN_HASH_" + UUID.randomUUID().toString().substring(0, 16);

            User defaultAdmin = User.builder()
                    .farmerId(farmerId)
                    .aadhaarHash(aadhaarHash)
                    .name(DEFAULT_ADMIN_NAME)
                    .phone(DEFAULT_ADMIN_PHONE)
                    .email(DEFAULT_ADMIN_EMAIL)
                    .state(DEFAULT_ADMIN_STATE)
                    .district(DEFAULT_ADMIN_DISTRICT)
                    .password(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                    .role(User.Role.ADMIN)
                    .isActive(true)
                    .build();

            userRepository.save(defaultAdmin);
            log.info("Default admin user created successfully with farmerId: {} and phone: {}", 
                    farmerId, DEFAULT_ADMIN_PHONE);
            log.info("Default admin password: {} (CHANGE THIS IN PRODUCTION)", DEFAULT_ADMIN_PASSWORD);

        } catch (Exception e) {
            log.error("Failed to initialize default admin user: {}", e.getMessage(), e);
        }
    }
}
