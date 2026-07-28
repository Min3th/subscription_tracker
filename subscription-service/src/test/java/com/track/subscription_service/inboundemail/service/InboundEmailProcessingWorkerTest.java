package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.entity.InboundEmail;
import com.track.subscription_service.inboundemail.model.InboundEmailStatus;
import com.track.subscription_service.inboundemail.repository.InboundEmailRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InboundEmailProcessingWorkerTest {
    private static final Instant NOW = Instant.parse("2026-07-28T04:00:00Z");

    @Test
    void claimsABoundedBatchAndProcessesOnlyItsClaimedEmails() {
        InboundEmailRepository repository = mock(InboundEmailRepository.class);
        InboundEmailProcessor processor = mock(InboundEmailProcessor.class);
        InboundEmailProperties properties = properties();
        InboundEmail email = mock(InboundEmail.class);
        when(repository.findClaimedBatch(anyString())).thenReturn(List.of(email));
        InboundEmailProcessingWorker worker = new InboundEmailProcessingWorker(
                repository, processor, properties, Clock.fixed(NOW, ZoneOffset.UTC));

        worker.processDueEmails();

        verify(repository).claimProcessingBatch(
                anyString(), eq(NOW), eq(NOW.minusSeconds(600)), eq(25));
        verify(processor).process(eq(email), anyString());
    }

    @Test
    void schedulesExponentialRetryWhenProcessingFails() {
        InboundEmailRepository repository = mock(InboundEmailRepository.class);
        InboundEmailProcessor processor = mock(InboundEmailProcessor.class);
        InboundEmail email = mock(InboundEmail.class);
        UUID id = UUID.randomUUID();
        when(email.getId()).thenReturn(id);
        when(email.getAttemptCount()).thenReturn(2);
        when(repository.findClaimedBatch(anyString())).thenReturn(List.of(email));
        doThrow(new IllegalStateException("temporary"))
                .when(processor).process(eq(email), anyString());
        InboundEmailProcessingWorker worker = new InboundEmailProcessingWorker(
                repository, processor, properties(), Clock.fixed(NOW, ZoneOffset.UTC));

        worker.processDueEmails();

        verify(repository).markFailed(
                eq(id), anyString(), eq(InboundEmailStatus.RETRY),
                eq("IllegalStateException"), eq(NOW.plusSeconds(120)), isNull());
    }

    private InboundEmailProperties properties() {
        InboundEmailProperties properties = new InboundEmailProperties();
        properties.setProcessingBatchSize(25);
        properties.setProcessingMaxAttempts(5);
        return properties;
    }
}
