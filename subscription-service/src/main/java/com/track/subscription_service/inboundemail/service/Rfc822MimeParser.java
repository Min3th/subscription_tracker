package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.dto.ParsedInboundEmail;
import jakarta.mail.Header;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Properties;

@Component
public class Rfc822MimeParser {
    private final InboundEmailProperties properties;

    public Rfc822MimeParser(InboundEmailProperties properties) {
        this.properties = properties;
    }

    public ParsedInboundEmail parse(byte[] rawMime) {
        if (rawMime == null || rawMime.length == 0) {
            throw new IllegalArgumentException("Inbound MIME message is empty");
        }
        if (rawMime.length > properties.getMaxRequestBytes()) {
            throw new IllegalArgumentException("Inbound MIME message exceeds the size limit");
        }
        try {
            MimeMessage message = new MimeMessage(
                    Session.getInstance(new Properties()),
                    new ByteArrayInputStream(rawMime));
            ParsedContent content = new ParsedContent();
            collect(message, content, new PartCounter());
            String headers = headers(message);
            return new ParsedInboundEmail(
                    null,
                    firstHeader(message, "From"),
                    firstHeader(message, "To"),
                    bounded(message.getSubject(), "subject"),
                    bounded(content.text, "text body"),
                    bounded(content.html, "HTML body"),
                    bounded(headers, "headers"),
                    null,
                    0);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (MessagingException | IOException exception) {
            throw new IllegalArgumentException("Invalid RFC 822/MIME message", exception);
        }
    }

    private void collect(Part part, ParsedContent content, PartCounter counter)
            throws MessagingException, IOException {
        counter.increment();
        String disposition = part.getDisposition();
        if (part.getFileName() != null
                || Part.ATTACHMENT.equalsIgnoreCase(disposition)
                || Part.INLINE.equalsIgnoreCase(disposition)
                    && !part.isMimeType("text/plain")
                    && !part.isMimeType("text/html")) {
            throw new IllegalArgumentException("Inbound email attachments are not supported");
        }
        if (part.isMimeType("multipart/*")) {
            jakarta.mail.Multipart multipart = (jakarta.mail.Multipart) part.getContent();
            for (int index = 0; index < multipart.getCount(); index++) {
                collect(multipart.getBodyPart(index), content, counter);
            }
            return;
        }
        if (part.isMimeType("message/rfc822")) {
            throw new IllegalArgumentException("Attached MIME messages are not supported");
        }
        if (part.isMimeType("text/plain") && content.text == null) {
            content.text = partText(part);
        } else if (part.isMimeType("text/html") && content.html == null) {
            content.html = partText(part);
        }
    }

    private String partText(Part part) throws MessagingException, IOException {
        Object value = part.getContent();
        if (value instanceof String text) {
            return text;
        }
        if (value instanceof java.io.InputStream input) {
            byte[] bytes = input.readNBytes(properties.getMaxFieldBytes() + 1);
            if (bytes.length > properties.getMaxFieldBytes()) {
                throw new IllegalArgumentException("Inbound email field exceeds the size limit");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return null;
    }

    private String headers(MimeMessage message) throws MessagingException {
        StringBuilder result = new StringBuilder();
        Enumeration<Header> headers = message.getAllHeaders();
        while (headers.hasMoreElements()) {
            Header header = headers.nextElement();
            result.append(header.getName())
                    .append(": ")
                    .append(header.getValue())
                    .append("\r\n");
            if (result.length() > properties.getMaxFieldBytes()) {
                throw new IllegalArgumentException("Inbound email headers exceed the size limit");
            }
        }
        return result.toString();
    }

    private String firstHeader(MimeMessage message, String name) throws MessagingException {
        String[] values = message.getHeader(name);
        return values == null || values.length == 0 ? null : values[0];
    }

    private String bounded(String value, String field) {
        if (value != null && value.length() > properties.getMaxFieldBytes()) {
            throw new IllegalArgumentException("Inbound email " + field + " exceeds the size limit");
        }
        return value;
    }

    private final class PartCounter {
        private int count;

        private void increment() {
            count++;
            if (count > properties.getMaxParts()) {
                throw new IllegalArgumentException("Inbound email has too many MIME parts");
            }
        }
    }

    private static final class ParsedContent {
        private String text;
        private String html;
    }
}
