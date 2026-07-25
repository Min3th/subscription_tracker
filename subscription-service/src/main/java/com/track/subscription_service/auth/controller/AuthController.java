package com.track.subscription_service.auth.controller;

import com.track.subscription_service.auth.dto.GoogleAuthRequest;
import com.track.subscription_service.auth.service.AuthService;
import com.track.subscription_service.auth.service.RefreshTokenService;
import com.track.subscription_service.auth.util.AuthResponse;
import com.track.subscription_service.config.FrontendOriginPolicy;
import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final FrontendOriginPolicy frontendOriginPolicy;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;
    public AuthController(AuthService authService,
                          RefreshTokenService refreshTokenService,
                          UserRepository userRepository,
                          FrontendOriginPolicy frontendOriginPolicy,
                          @Value("${app.auth.refresh-cookie.secure}") boolean refreshCookieSecure,
                          @Value("${app.auth.refresh-cookie.same-site}") String refreshCookieSameSite) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.frontendOriginPolicy = frontendOriginPolicy;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
        if ("None".equalsIgnoreCase(refreshCookieSameSite) && !refreshCookieSecure) {
            throw new IllegalStateException("SameSite=None refresh cookies must be Secure");
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@RequestBody GoogleAuthRequest request,
                                        HttpServletRequest servletRequest,
                                        HttpServletResponse response){
        requireAllowedOrigin(servletRequest);

        AuthResponse auth = authService.handleGoogleLogin(request.getCredential());

        setRefreshCookie(response, auth.getRefreshToken(), 7 * 24 * 60 * 60);

        return ResponseEntity.ok(new AuthResponse(auth.getAccessToken(),auth.getUser()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh (HttpServletRequest request, HttpServletResponse response){
        requireAllowedOrigin(request);
        String refreshToken = readRefreshToken(request);

        if (refreshToken == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "No refresh token available");
        }

        try {
            AuthResponse auth = refreshTokenService.rotate(refreshToken);
            setRefreshCookie(response, auth.getRefreshToken(), 7 * 24 * 60 * 60);
            return ResponseEntity.ok(new AuthResponse(auth.getAccessToken(), auth.getUser()));
        }
        catch (Exception e){
            clearRefreshCookie(response);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid refresh token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        requireAllowedOrigin(request);
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
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/auth")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        setRefreshCookie(response, "", 0);
    }

    private void requireAllowedOrigin(HttpServletRequest request) {
        frontendOriginPolicy.requireAllowedBrowserOrigin(request.getHeader(HttpHeaders.ORIGIN));
    }
}
