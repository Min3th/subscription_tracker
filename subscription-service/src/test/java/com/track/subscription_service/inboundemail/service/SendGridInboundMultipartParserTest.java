package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.dto.ParsedInboundEmail;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SendGridInboundMultipartParserTest {
    private static final String BOUNDARY = "subtrak-test-boundary";

    @Test
    void parsesKnownFieldsAndCountsAttachmentsWithoutRetainingTheirContent() {
        SendGridInboundMultipartParser parser = parser(4096, 1024, 20);
        byte[] body = multipart(
                field("envelope", "{\"from\":\"billing@example.com\",\"to\":[\"sub-token@example.com\"]}"),
                field("subject", "Your café subscription"),
                field("text", "Paid USD 12.99"),
                field("spam_score", "0.125"),
                field("unknown", "ignored"),
                file("attachment1", "invoice.pdf", "application/pdf", "untrusted-file-content")
        );

        ParsedInboundEmail parsed = parser.parse(body, contentType());

        assertEquals("Your café subscription", parsed.subject());
        assertEquals("Paid USD 12.99", parsed.text());
        assertEquals(new BigDecimal("0.125"), parsed.spamScore());
        assertEquals(1, parsed.attachmentCount());
        assertFalse(parsed.toString().contains("untrusted-file-content"));
    }

    @Test
    void rejectsDuplicateFieldsAndOversizedValues() {
        SendGridInboundMultipartParser parser = parser(4096, 8, 20);

        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                multipart(field("subject", "first"), field("subject", "second")),
                contentType()));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                multipart(field("subject", "more-than-eight-bytes")),
                contentType()));
    }

    @Test
    void rejectsNonMultipartAndOversizedRequests() {
        SendGridInboundMultipartParser parser = parser(128, 64, 20);

        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("plain".getBytes(StandardCharsets.UTF_8), "text/plain"));
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(new byte[129], contentType()));
    }

    private SendGridInboundMultipartParser parser(long requestBytes, int fieldBytes, int parts) {
        InboundEmailProperties properties = new InboundEmailProperties();
        properties.setMaxRequestBytes(requestBytes);
        properties.setMaxFieldBytes(fieldBytes);
        properties.setMaxParts(parts);
        return new SendGridInboundMultipartParser(properties);
    }

    private byte[] multipart(String... parts) {
        StringBuilder body = new StringBuilder();
        for (String part : parts) {
            body.append("--").append(BOUNDARY).append("\r\n").append(part);
        }
        body.append("--").append(BOUNDARY).append("--\r\n");
        return body.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String field(String name, String value) {
        return "Content-Disposition: form-data; name=\"" + name + "\"\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n\r\n"
                + value + "\r\n";
    }

    private String file(String name, String filename, String type, String value) {
        return "Content-Disposition: form-data; name=\"" + name + "\"; filename=\""
                + filename + "\"\r\n"
                + "Content-Type: " + type + "\r\n\r\n"
                + value + "\r\n";
    }

    private String contentType() {
        return "multipart/form-data; boundary=" + BOUNDARY;
    }
}
