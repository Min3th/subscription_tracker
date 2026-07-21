package com.track.subscription_service.auth.controller;

import com.track.subscription_service.auth.dto.GoogleAuthRequest;
import com.track.subscription_service.auth.service.AuthService;
import com.track.subscription_service.auth.service.RefreshTokenService;
import com.track.subscription_service.auth.util.AuthResponse;
import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "https://localhost:5173",allowCredentials = "true")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    public AuthController(AuthService authService,
                          RefreshTokenService refreshTokenService,
                          UserRepository userRepository) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@RequestBody GoogleAuthRequest request, HttpServletResponse response){

        AuthResponse auth = authService.handleGoogleLogin(request.getCredential());

        setRefreshCookie(response, auth.getRefreshToken(), 7 * 24 * 60 * 60);

        return ResponseEntity.ok(new AuthResponse(auth.getAccessToken(),auth.getUser()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh (HttpServletRequest request, HttpServletResponse response){
        String refreshToken = readRefreshToken(request);

        if (refreshToken == null){
            return ResponseEntity.status(401).body("No refresh token available!");
        }

        try {
            AuthResponse auth = refreshTokenService.rotate(refreshToken);
            setRefreshCookie(response, auth.getRefreshToken(), 7 * 24 * 60 * 60);
            return ResponseEntity.ok(new AuthResponse(auth.getAccessToken(), auth.getUser()));
        }
        catch (Exception e){
            clearRefreshCookie(response);
            return ResponseEntity.status(401).body("Invalid refresh token!");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = readRefreshToken(request);
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }
        clearRefreshCookie(response);
        return ResponseEntity.ok("Logged out successfully!");
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(Authentication authentication, HttpServletResponse response) {
        User user = userRepository.findByGoogleId(authentication.getName()).orElseThrow();
        refreshTokenService.revokeAllForUser(user);
        clearRefreshCookie(response);
        return ResponseEntity.ok("All sessions logged out successfully!");
    }

    private String readRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void setRefreshCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/auth")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        setRefreshCookie(response, "", 0);
    }
}
