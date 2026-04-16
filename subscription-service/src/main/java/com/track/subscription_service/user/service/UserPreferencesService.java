package com.track.subscription_service.user.service;

import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.entity.UserPreferences;
import com.track.subscription_service.user.repository.UserPreferencesRepository;
import com.track.subscription_service.user.repository.UserRepository;

public class UserPreferencesService {

    private final UserPreferencesRepository repo;
    private final UserRepository userRepository;

    public UserPreferencesService(UserPreferencesRepository repo,UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    public UserPreferences getByGoogleId(String googleId) {

        User user = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return repo.findByUser(user)
                .orElseGet(() -> createDefaultPreferences(user));
    }

    public UserPreferences create(UserPreferences userPreferences){
        return repo.save(userPreferences);
    }

    public UserPreferences updatePreferences(String googleId, UserPreferences updated) {

        UserPreferences existing = getByGoogleId(googleId);

        existing.setCurrency(updated.getCurrency());
        existing.setLanguage(updated.getLanguage());
        existing.setTimezone(updated.getTimezone());
        existing.setTheme(updated.getTheme());

        return repo.save(existing);
    }

    private UserPreferences createDefaultPreferences(User user) {

        UserPreferences prefs = new UserPreferences(
                user,
                "USD",
                "en",
                "UTC",
                "light"
        );

        return repo.save(prefs);
    }
}
