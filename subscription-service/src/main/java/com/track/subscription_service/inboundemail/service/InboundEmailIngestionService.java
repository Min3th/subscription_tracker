package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.dto.ParsedInboundEmail;
import com.track.subscription_service.inboundemail.dto.ProviderInboundEmail;
import com.track.subscription_service.inboundemail.entity.InboundEmailAddress;
import com.track.subscription_service.inboundemail.model.InboundIngestionResult;
import com.track.subscription_service.inboundemail.repository.InboundEmailAddressRepository;
import com.track.subscription_service.inboundemail.repository.InboundEmailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InboundEmailIngestionService {
    private static final Pattern MESSAGE_ID =
            Pattern.compile("(?im)^Message-ID:\\s*(\\S.*?)\\s*$");

    private final InboundEmailWebhookVerifier verifier;
    private final SendGridInboundMultipartParser parser;
    private final Rfc822MimeParser mimeParser;
    private final InboundEmailAddressRepository addressRepository;
    private final InboundEmailRepository emailRepository;
    private final InboundEmailTokenCodec tokenCodec;
    private final InboundEmailProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public InboundEmailIngestionService(InboundEmailWebhookVerifier verifier,
                                        SendGridInboundMultipartParser parser,
                                        Rfc822MimeParser mimeParser,
                                        InboundEmailAddressRepository addressRepository,
                                        InboundEmailRepository emailRepository,
                                        InboundEmailTokenCodec tokenCodec,
                                        InboundEmailProperties properties,
                                        ObjectMapper objectMapper,
                                        Clock clock) {
        this.verifier = verifier;
        this.parser = parser;
        this.mimeParser = mimeParser;
        this.addressRepository = addressRepository;
        this.emailRepository = emailRepository;
        this.tokenCodec = tokenCodec;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void receive(byte[] rawBody, String contentType, String signature, String timestamp) {
        verifier.verify(rawBody, signature, timestamp);
        ParsedInboundEmail parsed = parser.parse(rawBody, contentType);
        Envelope envelope = parseEnvelope(parsed.envelope());
        persist(
                messageId(parsed.headers()),
                envelope.from(),
                envelope.to(),
                parsed,
                null,
                null,
                clock.instant());
    }

    @Transactional
    public InboundIngestionResult ingest(ProviderInboundEmail inboundEmail) {
        if (inboundEmail == null) {
            throw new IllegalArgumentException("Inbound email is required");
        }
        requireSafeVirusVerdict(inboundEmail.virusVerdict());
        validateSpamVerdict(inboundEmail.spamVerdict());
        ParsedInboundEmail parsed = mimeParser.parse(inboundEmail.rawMime());
        return persist(
                bounded(inboundEmail.providerMessageId(), 998, "provider message ID"),
                inboundEmail.envelopeSender(),
                inboundEmail.envelopeRecipients(),
                parsed,
                normalizeVerdict(inboundEmail.spamVerdict()),
                normalizeVerdict(inboundEmail.virusVerdict()),
                inboundEmail.receivedAt() == null ? clock.instant() : inboundEmail.receivedAt());
    }

    private InboundIngestionResult persist(
            String providerMessageId,
            String envelopeSender,
            List<String> envelopeRecipients,
            ParsedInboundEmail parsed,
            String spamVerdict,
            String virusVerdict,
            java.time.Instant receivedAt) {
        String recipientToken = singleSubtrakRecipientToken(envelopeRecipients);
        if (recipientToken == null) {
            return InboundIngestionResult.DISCARDED;
        }

        InboundEmailAddress address = addressRepository
                .findByTokenHashAndRevokedAtIsNull(tokenCodec.hash(recipientToken))
                .orElse(null);
        if (address == null) {
            return InboundIngestionResult.DISCARDED;
        }

        String envelopeFrom = bounded(
                firstNonBlank(envelopeSender, parsed.from()), 320, "sender");
        String subject = bounded(parsed.subject(), 998, "subject");
        if (parsed.spamScore() != null && parsed.spamScore().signum() < 0) {
            throw new IllegalArgumentException("Inbound email spam score is invalid");
        }
        String mimeMessageId = messageId(parsed.headers());
        String fingerprintSource = mimeMessageId != null
                ? "message-id\0" + mimeMessageId
                : providerMessageId != null && !providerMessageId.isBlank()
                ? "provider-id\0" + providerMessageId
                : String.join("\0",
                        "content",
                        nullToEmpty(envelopeFrom),
                        nullToEmpty(subject),
                        nullToEmpty(parsed.text()),
                        nullToEmpty(parsed.html()),
                        nullToEmpty(parsed.headers()));
        String fingerprint = tokenCodec.hash(address.getId() + "\0" + fingerprintSource);

        int inserted = emailRepository.insertReceived(
                UUID.randomUUID(),
                address.getUser().getId(),
                address.getId(),
                providerMessageId,
                fingerprint,
                envelopeFrom,
                subject,
                parsed.text(),
                parsed.html(),
                parsed.headers(),
                parsed.spamScore(),
                spamVerdict,
                virusVerdict,
                receivedAt
        );
        return inserted == 1
                ? InboundIngestionResult.INSERTED
                : InboundIngestionResult.DUPLICATE;
    }

    private void requireSafeVirusVerdict(String verdict) {
        if (verdict == null || !"PASS".equalsIgnoreCase(verdict.strip())) {
            throw new IllegalArgumentException("Inbound email failed virus validation");
        }
    }

    private void validateSpamVerdict(String verdict) {
        if (verdict == null || verdict.isBlank()) {
            throw new IllegalArgumentException("Inbound email spam verdict is required");
        }
        String normalized = verdict.strip().toUpperCase(Locale.ROOT);
        if (!java.util.Set.of("PASS", "FAIL", "GRAY", "PROCESSING_FAILED")
                .contains(normalized)) {
            throw new IllegalArgumentException("Inbound email spam verdict is invalid");
        }
    }

    private String normalizeVerdict(String verdict) {
        return verdict == null ? null : verdict.strip().toUpperCase(Locale.ROOT);
    }

    private Envelope parseEnvelope(String rawEnvelope) {
        if (rawEnvelope == null || rawEnvelope.isBlank()) {
            throw new IllegalArgumentException("Inbound email envelope is required");
        }
        try {
            Envelope envelope = objectMapper.readValue(rawEnvelope, Envelope.class);
            if (envelope.to() == null || envelope.to().isEmpty()) {
                throw new IllegalArgumentException("Inbound email envelope recipient is required");
            }
            return envelope;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Inbound email envelope is invalid", exception);
        }
    }

    private String singleSubtrakRecipientToken(List<String> recipients) {
        String domain = Pattern.quote(properties.getDomain().toLowerCase(Locale.ROOT));
        Pattern addressPattern = Pattern.compile(
                "(?i)^sub-([A-Za-z0-9_-]{43})@" + domain + "$");
        String matchedToken = null;
        for (String recipient : recipients) {
            if (recipient == null) {
                continue;
            }
            Matcher matcher = addressPattern.matcher(extractAddress(recipient));
            if (matcher.matches()) {
                if (matchedToken != null && !matchedToken.equals(matcher.group(1))) {
                    return null;
                }
                matchedToken = matcher.group(1);
            }
        }
        return matchedToken;
    }

    private String extractAddress(String recipient) {
        String trimmed = recipient.trim();
        int start = trimmed.lastIndexOf('<');
        int end = trimmed.lastIndexOf('>');
        if (start >= 0 && end > start) {
            return trimmed.substring(start + 1, end).trim();
        }
        return trimmed;
    }

    private String messageId(String headers) {
        if (headers == null) {
            return null;
        }
        Matcher matcher = MESSAGE_ID.matcher(headers);
        return matcher.find() ? bounded(matcher.group(1), 998, "message ID") : null;
    }

    private String bounded(String value, int maxLength, String field) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException("Inbound email " + field + " exceeds the size limit");
        }
        return value;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record Envelope(String from, List<String> to) {
    }
}
