package com.track.subscription_service.auth.controller;

import com.track.subscription_service.auth.dto.GoogleAuthRequest;
import com.track.subscription_service.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@RequestBody GoogleAuthRequest request){
        return ResponseEntity.ok(authService.handleGoogleLogin(request.getCredential()));
    }
}