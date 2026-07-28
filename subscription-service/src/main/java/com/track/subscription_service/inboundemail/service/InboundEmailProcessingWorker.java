package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.entity.InboundEmail;
import com.track.subscription_service.inboundemail.model.InboundEmailStatus;
import com.track.subscription_service.inboundemail.repository.InboundEmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class InboundEmailProcessingWorker {
    private static final Logger log =
            LoggerFactory.getLogger(InboundEmailProcessingWorker.class);
    private static final Duration STALE_CLAIM_AGE = Duration.ofMinutes(10);
    private static final Duration BASE_RETRY_DELAY = Duration.ofMinutes(1);

    private final InboundEmailRepository emailRepository;
    private final InboundEmailProcessor processor;
    private final InboundEmailProperties properties;
    private final Clock clock;

    public InboundEmailProcessingWorker(
            InboundEmailRepository emailRepository,
            InboundEmailProcessor processor,
            InboundEmailProperties properties,
            Clock clock) {
        this.emailRepository = emailRepository;
        this.processor = processor;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${app.inbound-email.processing-delay-ms:30000}",
            initialDelayString = "${app.inbound-email.processing-initial-delay-ms:15000}")
    public void processDueEmails() {
        Instant now = clock.instant();
        String claimToken = UUID.randomUUID().toString();
        emailRepository.claimProcessingBatch(
                claimToken,
                now,
                now.minus(STALE_CLAIM_AGE),
                properties.getProcessingBatchSize());

        for (InboundEmail email : emailRepository.findClaimedBatch(claimToken)) {
            try {
                processor.process(email, claimToken);
            } catch (RuntimeException exception) {
                handleFailure(email, claimToken, exception);
            }
        }
    }

    private void handleFailure(
            InboundEmail email, String claimToken, RuntimeException exception) {
        Instant now = clock.instant();
        boolean exhausted =
                email.getAttemptCount() >= properties.getProcessingMaxAttempts();
        InboundEmailStatus status =
                exhausted ? InboundEmailStatus.DEAD : InboundEmailStatus.RETRY;
        Instant nextAttemptAt = exhausted ? null : now.plus(retryDelay(email.getAttemptCount()));
        String failureCode = exception.getClass().getSimpleName();
        if (failureCode.length() > 100) {
            failureCode = failureCode.substring(0, 100);
        }
        emailRepository.markFailed(
                email.getId(),
                claimToken,
                status,
                failureCode,
                nextAttemptAt,
                exhausted ? now : null);
        log.error("Inbound email processing failed for {}", email.getId(), exception);
    }

    private Duration retryDelay(int attemptCount) {
        int exponent = Math.max(0, Math.min(attemptCount - 1, 6));
        return BASE_RETRY_DELAY.multipliedBy(1L << exponent);
    }
}
