package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.repository.InboundEmailRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class InboundEmailRetentionWorkerTest {
    @Test
    void purgesOneBoundedBatchUsingTheConfiguredRetentionWindow() {
        Instant now = Instant.parse("2026-07-28T04:00:00Z");
        InboundEmailRepository repository = mock(InboundEmailRepository.class);
        InboundEmailProperties properties = new InboundEmailProperties();
        properties.setContentRetentionDays(30);
        properties.setRetentionBatchSize(75);
        when(repository.purgeExpiredContent(
                Instant.parse("2026-06-28T04:00:00Z"), now, 75))
                .thenReturn(12);
        InboundEmailRetentionWorker worker = new InboundEmailRetentionWorker(
                repository, properties, Clock.fixed(now, ZoneOffset.UTC));

        int purged = worker.purgeExpiredContent();

        assertEquals(12, purged);
        verify(repository).purgeExpiredContent(
                Instant.parse("2026-06-28T04:00:00Z"), now, 75);
        verifyNoMoreInteractions(repository);
    }
}
