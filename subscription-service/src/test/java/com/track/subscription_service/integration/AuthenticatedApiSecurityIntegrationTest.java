package com.track.subscription_service.integration;

import com.track.subscription_service.auth.service.JwtService;
import com.track.subscription_service.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "jwt.secret=test-jwt-secret-key-that-is-longer-than-32-bytes",
        "jwt.issuer=test-issuer",
        "jwt.audience=test-audience",
        "google.client.id=test-google-client-id",
        "app.sendgrid.apiKey=SGaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "app.sendgrid.fromEmail=test@example.com",
        "app.sendgrid.fromName=Subtrak Tests",
        "app.sendgrid.eventWebhookPublicKey="
})
class AuthenticatedApiSecurityIntegrationTest extends PostgresIntegrationTest {

    private static final String SECRET = "test-jwt-secret-key-that-is-longer-than-32-bytes";
    private static final String ISSUER = "test-issuer";
    private static final String AUDIENCE = "test-audience";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void validAccessTokenCanCallAProtectedApi() throws Exception {
        User user = new User();
        user.setGoogleId("authenticated-api-user");

        mockMvc.perform(get("/subscriptions")
                        .header("Authorization", bearer(jwtService.generateAccessToken(user))))
                .andExpect(status().isOk());
    }

    @Test
    void missingOrMalformedBearerCredentialsAreUnauthorized() throws Exception {
        assertUnauthorized(null);
        assertUnauthorized("Basic credentials");
        assertUnauthorized("Bearer ");
        assertUnauthorized("Bearer not-a-jwt");
    }

    @Test
    void refreshTokenCannotAuthenticateAProtectedApi() throws Exception {
        User user = new User();
        user.setGoogleId("refresh-token-user");

        assertUnauthorized(bearer(jwtService.generateRefreshToken(user, UUID.randomUUID().toString())));
    }

    @Test
    void expiredAccessTokenIsUnauthorized() throws Exception {
        assertUnauthorized(bearer(token(
                key(SECRET), ISSUER, AUDIENCE, "access",
                new Date(System.currentTimeMillis() - 60_000)
        )));
    }

    @Test
    void tokenWithWrongIssuerIsUnauthorized() throws Exception {
        assertUnauthorized(bearer(token(
                key(SECRET), "another-issuer", AUDIENCE, "access",
                new Date(System.currentTimeMillis() + 60_000)
        )));
    }

    @Test
    void tokenWithWrongAudienceIsUnauthorized() throws Exception {
        assertUnauthorized(bearer(token(
                key(SECRET), ISSUER, "another-audience", "access",
                new Date(System.currentTimeMillis() + 60_000)
        )));
    }

    @Test
    void tokenWithWrongSignatureIsUnauthorized() throws Exception {
        assertUnauthorized(bearer(token(
                key("another-test-secret-key-that-is-longer-than-32-bytes"),
                ISSUER, AUDIENCE, "access",
                new Date(System.currentTimeMillis() + 60_000)
        )));
    }

    private void assertUnauthorized(String authorization) throws Exception {
        var request = get("/subscriptions");
        if (authorization != null) {
            request.header("Authorization", authorization);
        }

        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("A valid access token is required"))
                .andExpect(jsonPath("$.path").value("/subscriptions"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    private String token(SecretKey signingKey,
                         String issuer,
                         String audience,
                         String type,
                         Date expiration) {
        return Jwts.builder()
                .setSubject("authenticated-api-user")
                .setId(UUID.randomUUID().toString())
                .setIssuer(issuer)
                .setAudience(audience)
                .claim("type", type)
                .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
                .setExpiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    private SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
