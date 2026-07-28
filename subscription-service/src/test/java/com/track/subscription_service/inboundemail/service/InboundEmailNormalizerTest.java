package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.dto.NormalizedInboundEmail;
import com.track.subscription_service.inboundemail.entity.InboundEmail;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InboundEmailNormalizerTest {

    @Test
    void prefersPlainTextAndRemovesForwardingMetadataAndQuotedReplies() {
        InboundEmail email = new InboundEmail();
        email.setSubject("  Fwd:   Receipt \r\n");
        email.setEnvelopeFrom("Customer <customer@GMAIL.com>");
        email.setTextBody("""
                ---------- Forwarded message ---------
                From: Netflix <info@netflix.com>
                Date: July 27, 2026
                Subject: Receipt
                Netflix renewal receipt
                Payment received: USD 22.99
                > old quoted message
                """);
        email.setHtmlBody("<p>This HTML must not win</p>");

        NormalizedInboundEmail normalized = new InboundEmailNormalizer().normalize(email);

        assertEquals("Fwd: Receipt", normalized.subject());
        assertEquals("gmail.com", normalized.senderDomain());
        assertEquals("Netflix renewal receipt\nPayment received: USD 22.99",
                normalized.body());
        assertFalse(normalized.body().contains("old quoted"));
        assertFalse(normalized.body().contains("HTML must not win"));
    }

    @Test
    void parsesHtmlOnlyEmailAsTextWithoutReturningMarkup() {
        InboundEmail email = new InboundEmail();
        email.setHtmlBody("""
                <html><body><script>alert('no')</script>
                <p>Payment received: <strong>EUR 9.99</strong></p></body></html>
                """);

        NormalizedInboundEmail normalized = new InboundEmailNormalizer().normalize(email);

        assertEquals("Payment received: EUR 9.99", normalized.body());
        assertFalse(normalized.body().contains("<strong>"));
        assertFalse(normalized.body().contains("alert"));
    }
}
