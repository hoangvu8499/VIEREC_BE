package com.vierec;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: the Spring context must start with every bean wired.
 */
@SpringBootTest
@ActiveProfiles("test")
class VierecApplicationTests {

    @Test
    void contextLoads() {
        // fails if any bean definition, property binding or config class is broken
    }
}
