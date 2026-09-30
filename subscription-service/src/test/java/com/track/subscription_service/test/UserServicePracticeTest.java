package com.track.subscription_service.test;

import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.repository.UserRepository;
import com.track.subscription_service.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServicePracticeTest {

    private UserRepository repository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        userService = new UserService(repository);
    }

    @Test
    void createSavesAndReturnsUser() {
        // Arrange: define the input and teach the mock what save() should return.
        User input = new User(
                "google-123",
                "alice@example.com",
                "Alice",
                "Google",
                Instant.parse("2026-08-24T00:00:00Z"),
                Instant.parse("2026-08-24T00:00:00Z")
        );
        User saved = new User(
                "google-123",
                "alice@example.com",
                "Alice",
                "Google",
                input.getCreatedAt(),
                input.getUpdatedAt()
        );
        saved.setId(42L);
        when(repository.save(input)).thenReturn(saved);

        // Act: invoke the real service method.
        User result = userService.create(input);

        // Assert: check both the returned state and the repository interaction.
        assertSame(saved, result);
        assertEquals(42L, result.getId());
        verify(repository).save(input);
    }
}
