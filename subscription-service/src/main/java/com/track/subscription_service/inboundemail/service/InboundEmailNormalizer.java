package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.dto.NormalizedInboundEmail;
import com.track.subscription_service.inboundemail.entity.InboundEmail;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.net.IDN;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class InboundEmailNormalizer {
    private static final Pattern EMAIL =
            Pattern.compile("(?i)(?:<)?[A-Z0-9._%+-]+@([A-Z0-9.-]+)(?:>)?");
    private static final Pattern WHITESPACE = Pattern.compile("[\\t\\x0B\\f ]+");
    private static final Pattern FORWARDING_METADATA = Pattern.compile(
            "(?i)^(from|sent|date|subject|to):\\s+.+$");

    public NormalizedInboundEmail normalize(InboundEmail email) {
        String sourceBody = hasText(email.getTextBody())
                ? email.getTextBody()
                : htmlToText(email.getHtmlBody());
        return new NormalizedInboundEmail(
                cleanSingleLine(email.getSubject()),
                senderDomain(email.getEnvelopeFrom()),
                cleanBody(sourceBody)
        );
    }

    private String htmlToText(String html) {
        return hasText(html) ? Jsoup.parse(html).text() : "";
    }

    private String cleanBody(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace('\u00A0', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        StringBuilder result = new StringBuilder(normalized.length());
        for (String rawLine : normalized.split("\n", -1)) {
            String line = WHITESPACE.matcher(rawLine).replaceAll(" ").trim();
            if (line.startsWith(">") || line.matches("^-{2,}\\s*Forwarded message\\s*-{2,}$")
                    || FORWARDING_METADATA.matcher(line).matches()) {
                continue;
            }
            if (!line.isEmpty()) {
                if (!result.isEmpty()) {
                    result.append('\n');
                }
                result.append(line);
            }
        }
        return result.toString();
    }

    private String cleanSingleLine(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = WHITESPACE.matcher(
                Normalizer.normalize(value, Normalizer.Form.NFKC)
                        .replace('\r', ' ')
                        .replace('\n', ' '))
                .replaceAll(" ")
                .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String senderDomain(String sender) {
        if (sender == null) {
            return null;
        }
        Matcher matcher = EMAIL.matcher(sender);
        if (!matcher.find()) {
            return null;
        }
        try {
            return IDN.toASCII(matcher.group(1))
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("\\.$", "");
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
