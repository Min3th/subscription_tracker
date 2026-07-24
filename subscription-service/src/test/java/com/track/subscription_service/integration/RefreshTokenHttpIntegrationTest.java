package com.track.subscription_service.integration;

import com.track.subscription_service.auth.service.JwtService;
import com.track.subscription_service.auth.service.RefreshTokenService;
import com.track.subscription_service.auth.util.AuthResponse;
import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "jwt.secret=test-jwt-secret-key-that-is-longer-than-32-bytes",
        "jwt.issuer=test-issuer",
        "jwt.audience=test-audience",
        "google.client.id=test-google-client-id",
        "app.frontend.origins=https://frontend.example",
        "app.auth.refresh-cookie.secure=true",
        "app.auth.refresh-cookie.same-site=None",
        "app.sendgrid.apiKey=SGaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "app.sendgrid.fromEmail=test@example.com",
        "app.sendgrid.fromName=Subtrak Tests",
        "app.sendgrid.eventWebhookPublicKey="
})
class RefreshTokenHttpIntegrationTest extends PostgresIntegrationTest {

    private static final String GOOGLE_ID = "refresh-http-user";
    private static final String ALLOWED_ORIGIN = "https://frontend.example";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtService jwtService;

    private User user;
    private String refreshToken;

    @BeforeEach
    void createRefreshSession() {
        user = new User();
        user.setGoogleId(GOOGLE_ID);
        user.setEmail("refresh-http@example.com");
        user.setName("Refresh HTTP User");
        user = userRepository.saveAndFlush(user);
        refreshToken = refreshTokenService.createSession(user).getRefreshToken();
    }

    @AfterEach
    void removeTestData() {
        jdbc.update("DELETE FROM users WHERE google_id = ?", GOOGLE_ID);
    }

    @Test
    void refreshRotatesTheCookieAndRevokesThePresentedSession() throws Exception {
        MvcResult result = refresh(refreshToken, ALLOWED_ORIGIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=None")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=604800")))
                .andReturn();

        String replacement = refreshCookieValue(result);
        assertNotEquals(refreshToken, replacement);
        assertEquals(2, sessionCount());
        assertEquals(1, revokedSessionCount());
        assertEquals(1, activeSessionCount());

        refresh(replacement, ALLOWED_ORIGIN)
                .andExpect(status().isOk());
    }

    @Test
    void replayingARotatedTokenRevokesItsReplacementSessionFamily() throws Exception {
        String replacement = refreshCookieValue(
                refresh(refreshToken, ALLOWED_ORIGIN)
                        .andExpect(status().isOk())
                        .andReturn()
        );

        refresh(refreshToken, ALLOWED_ORIGIN)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        assertEquals(0, activeSessionCount());
        refresh(replacement, ALLOWED_ORIGIN)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesThePresentedSessionAndClearsTheCookie() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .cookie(refreshCookie(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=None")));

        assertEquals(0, activeSessionCount());
        refresh(refreshToken, ALLOWED_ORIGIN)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutAllRevokesEverySessionForTheAuthenticatedUser() throws Exception {
        String secondRefreshToken = refreshTokenService.createSession(user).getRefreshToken();

        mockMvc.perform(post("/auth/logout-all")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtService.generateAccessToken(user)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        assertEquals(0, activeSessionCount());
        refresh(refreshToken, ALLOWED_ORIGIN).andExpect(status().isUnauthorized());
        refresh(secondRefreshToken, ALLOWED_ORIGIN).andExpect(status().isUnauthorized());
    }

    @Test
    void browserOriginMustMatchTheConfiguredFrontend() throws Exception {
        refresh(refreshToken, "https://attacker.example")
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

        assertEquals(1, activeSessionCount());
        refresh(refreshToken, ALLOWED_ORIGIN)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void nonBrowserClientMayRefreshWithoutAnOriginHeader() throws Exception {
        refresh(refreshToken, null)
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String token, String origin)
            throws Exception {
        var request = post("/auth/refresh").cookie(refreshCookie(token));
        if (origin != null) {
            request.header(HttpHeaders.ORIGIN, origin);
        }
        return mockMvc.perform(request);
    }

    private Cookie refreshCookie(String token) {
        return new Cookie("refreshToken", token);
    }

    private String refreshCookieValue(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String prefix = "refreshToken=";
        int start = setCookie.indexOf(prefix) + prefix.length();
        int end = setCookie.indexOf(';', start);
        return setCookie.substring(start, end);
    }

    private int sessionCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM refresh_token_sessions WHERE user_id = ?",
                Integer.class, user.getId());
    }

    private int activeSessionCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM refresh_token_sessions WHERE user_id = ? AND revoked_at IS NULL",
                Integer.class, user.getId());
    }

    private int revokedSessionCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM refresh_token_sessions WHERE user_id = ? AND revoked_at IS NOT NULL",
                Integer.class, user.getId());
    }
}
