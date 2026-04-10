package com.track.subscription_service.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.track.subscription_service.auth.AuthResponse;
import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

@Service
public class AuthService {

    @Value("${google.client.id}")
    private String clientId;

    private final UserRepository userRepository;
    private final JwtService jwtService;

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory()
            )
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken != null) {
                return idToken.getPayload();
            } else {
                throw new RuntimeException("Invalid Google token");
            }

        } catch (Exception e) {
            throw new RuntimeException("Token verification failed: ", e);
        }
    }



    public AuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public AuthResponse handleGoogleLogin(String credential) {

        GoogleIdToken.Payload payload = verifyGoogleToken(credential);

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        Optional<User> existingUser = userRepository.findByGoogleId(googleId);

        User user;

        if (existingUser.isPresent()) {

            user = existingUser.get();
            user.setUpdatedAt(Instant.now());
        } else {

            user = new User();
            user.setGoogleId(googleId);
            user.setEmail(email);
            user.setName(name);
            user.setCreatedAt(Instant.now());
            user.setUpdatedAt(Instant.now());
        }

        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, user);
    }
}
