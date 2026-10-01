package com.track.subscription_service.health;

import io.swagger.v3.oas.annotations.Hidden;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Semaphore;
import javax.sql.DataSource;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
public class HealthController {
    private final ApplicationAvailability availability;
    private final DataSource dataSource;
    // Concurrent public probes must not exhaust the application connection pool.
    private final Semaphore databaseProbe = new Semaphore(1);

    public HealthController(ApplicationAvailability availability, DataSource dataSource) {
        this.availability = availability;
        this.dataSource = dataSource;
    }

    @GetMapping("/health/live")
    public ResponseEntity<Map<String, String>> live() {
        return response(availability.getLivenessState() == LivenessState.CORRECT);
    }

    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, String>> ready() {
        if (availability.getLivenessState() != LivenessState.CORRECT
                || availability.getReadinessState() != ReadinessState.ACCEPTING_TRAFFIC
                || !databaseProbe.tryAcquire()) {
            return response(false);
        }
        // Pool acquisition is bounded by Hikari's connection-timeout; JDBC validation
        // has its own one-second timeout. No new pool or background task per request.
        try (Connection connection = dataSource.getConnection()) {
            return response(connection.isValid(1));
        } catch (SQLException | RuntimeException unavailable) {
            return response(false);
        } finally {
            databaseProbe.release();
        }
    }

    private ResponseEntity<Map<String, String>> response(boolean healthy) {
        return ResponseEntity.status(healthy ? 200 : 503)
                .cacheControl(CacheControl.noStore())
                .body(Map.of("status", healthy ? "UP" : "DOWN"));
    }
}
