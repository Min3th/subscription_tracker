package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.dto.ParsedInboundEmail;
import com.track.subscription_service.inboundemail.entity.InboundEmailAddress;
import com.track.subscription_service.inboundemail.repository.InboundEmailAddressRepository;
import com.track.subscription_service.inboundemail.repository.InboundEmailRepository;
import com.track.subscription_service.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InboundEmailIngestionServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-28T04:00:00Z");
    private static final String TOKEN = "a".repeat(43);
    private static final byte[] RAW_BODY = new byte[] {0, 1, 2, (byte) 0xFF};
    private static final String CONTENT_TYPE = "multipart/form-data; boundary=test";

    @Test
    void verifiesBeforeParsingAndPersistsAReceivedEvent() {
        Fixture fixture = fixture(parsed(
                """
                {"from":"billing@example.com","to":["sub-%s@inbound.subtrak.me"]}
                """.formatted(TOKEN)));

        fixture.service.receive(RAW_BODY, CONTENT_TYPE, "signature", "timestamp");

        var order = inOrder(fixture.verifier, fixture.parser, fixture.addressRepository,
                fixture.emailRepository);
        order.verify(fixture.verifier).verify(RAW_BODY, "signature", "timestamp");
        order.verify(fixture.parser).parse(RAW_BODY, CONTENT_TYPE);
        order.verify(fixture.addressRepository)
                .findByTokenHashAndRevokedAtIsNull("token-hash");
        order.verify(fixture.emailRepository).insertReceived(
                any(), eq(11L), eq(7L), eq("<message-123@example.com>"),
                matches("[0-9a-f]{64}"), eq("billing@example.com"),
                eq("Receipt"), eq("Paid USD 12.99"), isNull(),
                eq("Message-ID: <message-123@example.com>\r\n"),
                eq(new BigDecimal("0.1")), eq(NOW));
    }

    @Test
    void producesTheSameFingerprintForProviderRetries() {
        Fixture fixture = fixture(parsed(
                """
                {"from":"billing@example.com","to":["sub-%s@inbound.subtrak.me"]}
                """.formatted(TOKEN)));

        fixture.service.receive(RAW_BODY, CONTENT_TYPE, "signature", "timestamp");
        fixture.service.receive(RAW_BODY, CONTENT_TYPE, "signature", "timestamp");

        ArgumentCaptor<String> fingerprints = ArgumentCaptor.forClass(String.class);
        verify(fixture.emailRepository, times(2)).insertReceived(
                any(), anyLong(), anyLong(), any(), fingerprints.capture(),
                any(), any(), any(), any(), any(), any(), any());
        assertEquals(fingerprints.getAllValues().get(0), fingerprints.getAllValues().get(1));
    }

    @Test
    void silentlyAcknowledgesUnknownRevokedAndAmbiguousRecipients() {
        Fixture unknown = fixture(parsed(
                """
                {"to":["sub-%s@inbound.subtrak.me"]}
                """.formatted(TOKEN)));
        when(unknown.addressRepository.findByTokenHashAndRevokedAtIsNull("token-hash"))
                .thenReturn(Optional.empty());

        unknown.service.receive(RAW_BODY, CONTENT_TYPE, "signature", "timestamp");
        verifyNoInteractions(unknown.emailRepository);

        Fixture ambiguous = fixture(parsed(
                """
                {"to":["sub-%s@inbound.subtrak.me","sub-%s@inbound.subtrak.me"]}
                """.formatted(TOKEN, "b".repeat(43))));

        ambiguous.service.receive(RAW_BODY, CONTENT_TYPE, "signature", "timestamp");
        verifyNoInteractions(ambiguous.addressRepository, ambiguous.emailRepository);
    }

    @Test
    void rejectsMalformedEnvelopesAndOversizedDatabaseFields() {
        Fixture malformed = fixture(parsed("{not-json"));
        assertThrows(IllegalArgumentException.class, () -> malformed.service.receive(
                RAW_BODY, CONTENT_TYPE, "signature", "timestamp"));

        ParsedInboundEmail oversized = new ParsedInboundEmail(
                """
                {"to":["sub-%s@inbound.subtrak.me"]}
                """.formatted(TOKEN),
                null, null, "x".repeat(999), null, null, null, null, 0);
        Fixture oversizedFixture = fixture(oversized);
        assertThrows(IllegalArgumentException.class, () -> oversizedFixture.service.receive(
                RAW_BODY, CONTENT_TYPE, "signature", "timestamp"));
        verifyNoInteractions(oversizedFixture.emailRepository);
    }

    private Fixture fixture(ParsedInboundEmail parsed) {
        InboundEmailWebhookVerifier verifier = mock(InboundEmailWebhookVerifier.class);
        SendGridInboundMultipartParser parser = mock(SendGridInboundMultipartParser.class);
        InboundEmailAddressRepository addressRepository =
                mock(InboundEmailAddressRepository.class);
        InboundEmailRepository emailRepository = mock(InboundEmailRepository.class);
        InboundEmailTokenCodec tokenCodec = mock(InboundEmailTokenCodec.class);
        InboundEmailProperties properties = new InboundEmailProperties();
        properties.setDomain("inbound.subtrak.me");
        when(parser.parse(RAW_BODY, CONTENT_TYPE)).thenReturn(parsed);
        when(tokenCodec.hash(TOKEN)).thenReturn("token-hash");
        when(tokenCodec.hash(argThat(value -> value != null && value.startsWith("7\0"))))
                .thenReturn("f".repeat(64));

        User user = mock(User.class);
        when(user.getId()).thenReturn(11L);
        InboundEmailAddress address = mock(InboundEmailAddress.class);
        when(address.getId()).thenReturn(7L);
        when(address.getUser()).thenReturn(user);
        when(addressRepository.findByTokenHashAndRevokedAtIsNull("token-hash"))
                .thenReturn(Optional.of(address));

        InboundEmailIngestionService service = new InboundEmailIngestionService(
                verifier, parser, addressRepository, emailRepository, tokenCodec,
                properties, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
        return new Fixture(service, verifier, parser, addressRepository, emailRepository);
    }

    private ParsedInboundEmail parsed(String envelope) {
        return new ParsedInboundEmail(
                envelope,
                "visible-sender@example.com",
                null,
                "Receipt",
                "Paid USD 12.99",
                null,
                "Message-ID: <message-123@example.com>\r\n",
                new BigDecimal("0.1"),
                0
        );
    }

    private record Fixture(
            InboundEmailIngestionService service,
            InboundEmailWebhookVerifier verifier,
            SendGridInboundMultipartParser parser,
            InboundEmailAddressRepository addressRepository,
            InboundEmailRepository emailRepository
    ) {
    }
}
