package com.track.subscription_service.user.service;

import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.entity.UserPreferences;
import com.track.subscription_service.user.dto.UpdateUserPreferencesRequest;
import com.track.subscription_service.user.repository.UserPreferencesRepository;
import com.track.subscription_service.user.repository.UserRepository;
import com.track.subscription_service.common.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import com.track.subscription_service.notification.service.ReminderScheduleService;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;

@Service
public class UserPreferencesService {

    private final UserPreferencesRepository repo;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ReminderScheduleService reminderScheduleService;

    public UserPreferencesService(UserPreferencesRepository repo, UserRepository userRepository,
                                  SubscriptionRepository subscriptionRepository,
                                  ReminderScheduleService reminderScheduleService) {
        this.repo = repo;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.reminderScheduleService = reminderScheduleService;
    }

    public UserPreferences getByGoogleId(String googleId) {

        User user = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return repo.findByUser(user)
                .orElseGet(() -> createDefaultPreferences(user));
    }

    public UserPreferences create(UserPreferences userPreferences){
        return repo.save(userPreferences);
    }

    public UserPreferences updatePreferences(String googleId, UpdateUserPreferencesRequest updated) {

        UserPreferences existing = getByGoogleId(googleId);

        existing.setCurrency(updated.currency());
        existing.setLanguage(updated.language());
        existing.setTimezone(updated.timezone());
        existing.setTheme(updated.theme());
        existing.setEmailNotificationsEnabled(updated.emailNotificationsEnabled());
        existing.setReminderDaysBefore(updated.reminderDaysBefore());
        existing.setReminderTime(updated.reminderTime());

        UserPreferences saved = repo.save(existing);
        subscriptionRepository.findByUser_GoogleId(googleId).forEach(reminderScheduleService::refresh);
        return saved;
    }

    private UserPreferences createDefaultPreferences(User user) {

        UserPreferences prefs = new UserPreferences(
                user,
                "USD",
                "en",
                "UTC",
                "light",
                true,
                3        );

        UserPreferences saved = repo.save(prefs);
        subscriptionRepository.findByUser_GoogleId(user.getGoogleId()).forEach(reminderScheduleService::refresh);
        return saved;
    }
}
