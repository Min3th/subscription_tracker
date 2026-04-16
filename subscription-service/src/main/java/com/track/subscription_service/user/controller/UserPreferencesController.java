package com.track.subscription_service.user.controller;

import com.track.subscription_service.user.service.UserPreferencesService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/preferences")
public class UserPreferencesController {
    private final UserPreferencesService userPreferencesService;

    public UserPreferencesController(UserPreferencesService userPreferencesService) {
        this.userPreferencesService = userPreferencesService;
    }
}
