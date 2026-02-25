package com.farmer.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic application context test.
 */
@SpringBootTest(classes = UserServiceApplication.class)
@ActiveProfiles("test")
class UserServiceApplicationTest {

    @Test
    void contextLoads() {
        // Verify application context loads successfully
    }
}