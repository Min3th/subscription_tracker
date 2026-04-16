package com.track.subscription_service.user.controller;

import com.track.subscription_service.user.entity.UserPreferences;
import com.track.subscription_service.user.service.UserPreferencesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/user/preferences")
public class UserPreferencesController {
    private final UserPreferencesService userPreferencesService;

    public UserPreferencesController(UserPreferencesService userPreferencesService) {
        this.userPreferencesService = userPreferencesService;
    }

    @GetMapping
    public ResponseEntity<?> getPreferences(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String googleId = auth.getName();
        return ResponseEntity.ok(
                userPreferencesService.getByGoogleId(googleId)
        );
    }

    @PutMapping
    public ResponseEntity<?> updatePreferences(@RequestBody UserPreferences updated){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String googleId = auth.getName();

        return ResponseEntity.ok(
                userPreferencesService.updatePreferences(googleId,updated)
        );
    }
}
