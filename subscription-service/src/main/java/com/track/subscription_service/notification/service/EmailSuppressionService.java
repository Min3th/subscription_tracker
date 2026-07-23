package com.track.subscription_service.notification.service;

import com.track.subscription_service.notification.repository.EmailSuppressionRepository;
import com.track.subscription_service.notification.repository.NotificationDeliveryRepository;
import com.track.subscription_service.notification.repository.SubscriptionReminderScheduleRepository;
import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.repository.UserPreferencesRepository;
import com.track.subscription_service.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Service
public class EmailSuppressionService {
    private final EmailSuppressionRepository suppressionRepository;
    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final SubscriptionReminderScheduleRepository scheduleRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final Clock clock;

    public EmailSuppressionService(EmailSuppressionRepository suppressionRepository,
                                   UserRepository userRepository,
                                   UserPreferencesRepository preferencesRepository,
                                   SubscriptionReminderScheduleRepository scheduleRepository,
                                   NotificationDeliveryRepository deliveryRepository, Clock clock) {
        this.suppressionRepository = suppressionRepository;
        this.userRepository = userRepository;
        this.preferencesRepository = preferencesRepository;
        this.scheduleRepository = scheduleRepository;
        this.deliveryRepository = deliveryRepository;
        this.clock = clock;
    }

    public boolean isSuppressed(String email) {
        return email == null || suppressionRepository.existsByEmailNormalized(normalize(email));
    }

    @Transactional
    public void suppress(String email, String reason, String source) {
        String normalized = normalize(email);
        Instant now = clock.instant();
        suppressionRepository.upsert(normalized, reason, source, now);
        for (User user : userRepository.findAllByEmailIgnoreCase(normalized)) {
            preferencesRepository.findByUser(user).ifPresent(preferences -> {
                preferences.setEmailNotificationsEnabled(false);
                preferencesRepository.save(preferences);
            });
            scheduleRepository.deleteAllByUserId(user.getId());
            deliveryRepository.markOpenDeliveriesDeadForUser(
                    user.getId(), now, "Email suppressed: " + reason
            );
        }
    }

    private String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
