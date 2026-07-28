package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Rfc822MimeParserTest {
    private InboundEmailProperties properties;
    private Rfc822MimeParser parser;

    @BeforeEach
    void setUp() {
        properties = new InboundEmailProperties();
        properties.setMaxRequestBytes(10 * 1024 * 1024);
        properties.setMaxFieldBytes(1024 * 1024);
        properties.setMaxParts(30);
        parser = new Rfc822MimeParser(properties);
    }

    @Test
    void parsesEncodedSubjectAndMultipartAlternative() {
        var parsed = parser.parse(mime("""
                From: Billing <billing@example.com>
                To: somebody-else@example.com
                Message-ID: <mime-123@example.com>
                Subject: =?UTF-8?B?UmVjZWlwdCDigJMg4oKsMTIuOTk=?=
                MIME-Version: 1.0
                Content-Type: multipart/alternative; boundary="alternative"

                --alternative
                Content-Type: text/plain; charset=UTF-8

                Paid EUR 12.99
                --alternative
                Content-Type: text/html; charset=UTF-8

                <p>Paid <strong>EUR 12.99</strong></p>
                --alternative--
                """));

        assertEquals("Receipt – €12.99", parsed.subject());
        assertEquals("Paid EUR 12.99", parsed.text().strip());
        assertTrue(parsed.html().contains("<strong>EUR 12.99</strong>"));
        assertTrue(parsed.headers().contains("Message-ID: <mime-123@example.com>"));
    }

    @Test
    void rejectsAttachmentsOversizedMessagesAndExcessiveParts() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(mime("""
                From: sender@example.com
                MIME-Version: 1.0
                Content-Type: multipart/mixed; boundary="mixed"

                --mixed
                Content-Type: text/plain

                Body
                --mixed
                Content-Type: application/pdf
                Content-Disposition: attachment; filename="invoice.pdf"

                fake
                --mixed--
                """)));

        properties.setMaxRequestBytes(1024);
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(new byte[1025]));

        properties.setMaxRequestBytes(10 * 1024 * 1024);
        properties.setMaxParts(1);
        assertThrows(IllegalArgumentException.class, () -> parser.parse(mime("""
                MIME-Version: 1.0
                Content-Type: multipart/alternative; boundary="many"

                --many
                Content-Type: text/plain

                Body
                --many--
                """)));
    }

    private byte[] mime(String value) {
        return value.replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8);
    }
}
