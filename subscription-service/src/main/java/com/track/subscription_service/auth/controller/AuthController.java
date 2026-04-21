package com.track.subscription_service.auth.controller;

import com.track.subscription_service.auth.dto.GoogleAuthRequest;
import com.track.subscription_service.auth.service.AuthService;
import com.track.subscription_service.auth.service.JwtService;
import com.track.subscription_service.auth.util.AuthResponse;
import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173",allowCredentials = "true")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    public AuthController(AuthService authService,
                          JwtService jwtService,
                          UserRepository userRepository) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@RequestBody GoogleAuthRequest request, HttpServletResponse response){

        AuthResponse auth = authService.handleGoogleLogin(request.getCredential());

        Cookie cookie = new Cookie("refreshToken",auth.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(7*24*60*60);

        response.addCookie(cookie);

        return ResponseEntity.ok(new AuthResponse(auth.getAccessToken(),auth.getUser()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh (HttpServletRequest request){
        String refreshToken = null;

        if (request.getCookies() != null){
            for (Cookie cookie: request.getCookies()){
                if ("refreshToken".equals(cookie.getName())){
                    refreshToken = cookie.getValue();
                }
            }
        }

        if (refreshToken == null){
            return ResponseEntity.status(401).body("No refresh token available!");
        }

        try {
            Claims claims = jwtService.extractAllClaims(refreshToken);

            String type = claims.get("type", String.class);
            if (!"refresh".equals(type)) {
                return ResponseEntity.status(401).body("Invalid token type!");
            }

            String googleId = claims.getSubject();

            User user = userRepository.findByGoogleId(googleId).orElseThrow();

            String newAccessToken = jwtService.generateAccessToken(user);

            return ResponseEntity.ok(new AuthResponse(newAccessToken,user));
        }
        catch (Exception e){
            return ResponseEntity.status(401).body("Invalid refresh token!");
        }
    }
}