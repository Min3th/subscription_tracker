package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.repository.InboundEmailRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

@Component
public class InboundEmailRetentionWorker {
    private final InboundEmailRepository emailRepository;
    private final InboundEmailProperties properties;
    private final Clock clock;

    public InboundEmailRetentionWorker(InboundEmailRepository emailRepository,
                                       InboundEmailProperties properties,
                                       Clock clock) {
        this.emailRepository = emailRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.inbound-email.retention-cron:0 15 * * * *}")
    @Transactional
    public int purgeExpiredContent() {
        var now = clock.instant();
        var cutoff = now.minus(properties.getContentRetentionDays(), ChronoUnit.DAYS);
        return emailRepository.purgeExpiredContent(
                cutoff,
                now,
                properties.getRetentionBatchSize()
        );
    }
}
