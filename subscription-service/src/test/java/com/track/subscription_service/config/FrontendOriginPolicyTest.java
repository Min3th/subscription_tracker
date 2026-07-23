package com.track.subscription_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FrontendOriginPolicyTest {

    @Test
    void parsesNormalizesAndAllowsConfiguredOrigins() {
        FrontendOriginPolicy policy = new FrontendOriginPolicy(
                " https://app.example.com/,https://admin.example.com "
        );

        assertEquals(
                List.of("https://app.example.com", "https://admin.example.com"),
                policy.allowedOrigins()
        );
        assertDoesNotThrow(() -> policy.requireAllowedBrowserOrigin("https://app.example.com"));
        assertDoesNotThrow(() -> policy.requireAllowedBrowserOrigin(null));
    }

    @Test
    void rejectsAnUnconfiguredBrowserOrigin() {
        FrontendOriginPolicy policy = new FrontendOriginPolicy("https://app.example.com");

        assertThrows(
                ResponseStatusException.class,
                () -> policy.requireAllowedBrowserOrigin("https://evil.example")
        );
    }
}
