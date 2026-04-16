package com.track.subscription_service.user.service;

import com.track.subscription_service.user.entity.UserPreferences;
import com.track.subscription_service.user.repository.UserPreferencesRepository;

public class UserPreferencesService {

    private final UserPreferencesRepository repo;

    public UserPreferencesService(UserPreferencesRepository repo) {
        this.repo = repo;
    }

    public UserPreferences create(UserPreferences userPreferences){
        return repo.save(userPreferences);
    }
}
