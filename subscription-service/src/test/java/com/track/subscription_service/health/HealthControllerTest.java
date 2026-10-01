package com.track.subscription_service.health;

import com.track.subscription_service.auth.service.JwtService;
import com.track.subscription_service.auth.util.JwtAuthFilter;
import com.track.subscription_service.config.FrontendOriginPolicy;
import com.track.subscription_service.config.SecurityConfig;
import com.track.subscription_service.config.SecurityErrorHandler;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, JwtAuthFilter.class, HealthControllerTest.TestSecurity.class})
class HealthControllerTest {
    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    static class TestSecurity { }
    @Autowired MockMvc mvc;
    @MockitoBean ApplicationAvailability availability;
    @MockitoBean DataSource dataSource;
    @MockitoBean JwtService jwtService;
    @MockitoBean FrontendOriginPolicy frontendOriginPolicy;

    @BeforeEach
    void running() {
        when(availability.getLivenessState()).thenReturn(LivenessState.CORRECT);
        when(availability.getReadinessState()).thenReturn(ReadinessState.ACCEPTING_TRAFFIC);
    }

    @Test
    void livenessIsPublicMinimalAndNeverContactsDatabase() throws Exception {
        mvc.perform(get("/health/live"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$", aMapWithSize(1)))
                .andExpect(jsonPath("$.status").value("UP"));
        verifyNoInteractions(dataSource);
    }

    @Test
    void readinessValidatesAndReturnsConnectionToPool() throws Exception {
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(true);
        mvc.perform(get("/health/ready"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$", aMapWithSize(1)))
                .andExpect(jsonPath("$.status").value("UP"));
        verify(connection).close();
    }

    @Test
    void databaseOutageDoesNotFailLivenessOrDiscloseErrorsAndCanRecover() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.isValid(1)).thenReturn(true);
        when(dataSource.getConnection()).thenThrow(new SQLException("secret-host password=secret"))
                .thenReturn(connection);
        mvc.perform(get("/health/ready"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$", aMapWithSize(1)))
                .andExpect(jsonPath("$.status").value("DOWN"));
        mvc.perform(get("/health/live")).andExpect(status().isOk());
        mvc.perform(get("/health/ready")).andExpect(status().isOk());
    }

    @Test
    void invalidConnectionIsDownAndStillClosed() throws Exception {
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(false);
        mvc.perform(get("/health/ready")).andExpect(status().isServiceUnavailable());
        verify(connection).close();
    }

    @Test
    void shuttingDownDoesNotProbeDatabase() throws Exception {
        when(availability.getReadinessState()).thenReturn(ReadinessState.REFUSING_TRAFFIC);
        mvc.perform(get("/health/ready")).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/health/live")).andExpect(status().isOk());
        verifyNoInteractions(dataSource);
    }

    @Test
    void brokenApplicationFailsBothChecksWithoutQueryingDatabase() throws Exception {
        when(availability.getLivenessState()).thenReturn(LivenessState.BROKEN);
        mvc.perform(get("/health/live")).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/health/ready")).andExpect(status().isServiceUnavailable());
        verifyNoInteractions(dataSource);
    }

    @Test
    void publicAccessDoesNotExtendToOtherRoutes() throws Exception {
        mvc.perform(get("/subscriptions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/health/details")).andExpect(status().isUnauthorized());
        mvc.perform(post("/health/ready")).andExpect(status().isUnauthorized());
        verifyNoInteractions(dataSource);
    }
}
