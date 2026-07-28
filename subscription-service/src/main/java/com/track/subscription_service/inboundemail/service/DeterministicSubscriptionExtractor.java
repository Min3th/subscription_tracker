package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.dto.NormalizedInboundEmail;
import com.track.subscription_service.inboundemail.dto.SubscriptionExtraction;
import com.track.subscription_service.inboundemail.model.InboundEmailClassification;
import com.track.subscription_service.subscription.model.BillingUnit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeterministicSubscriptionExtractor {
    private static final Map<String, String> PROVIDERS = new LinkedHashMap<>();
    private static final Set<String> PERSONAL_EMAIL_DOMAINS = Set.of(
            "gmail.com", "googlemail.com", "outlook.com", "hotmail.com",
            "live.com", "yahoo.com", "icloud.com"
    );
    private static final Set<String> GMAIL_VERIFICATION_SENDERS = Set.of(
            "google.com", "googlemail.com"
    );
    private static final Pattern HTTPS_URL = Pattern.compile(
            "https://[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);
    private static final Pattern CODE_MONEY = Pattern.compile(
            "(?i)\\b(USD|EUR|GBP|LKR|JPY|CAD|AUD|INR)\\s*"
                    + "([0-9][0-9,]*(?:\\.[0-9]{1,4})?)\\b");
    private static final Pattern REVERSED_CODE_MONEY = Pattern.compile(
            "(?i)(?:[$€£₹]\\s*)?([0-9][0-9,]*(?:\\.[0-9]{1,4})?)\\s*"
                    + "\\b(USD|EUR|GBP|LKR|JPY|CAD|AUD|INR)\\b");
    private static final Pattern SYMBOL_MONEY = Pattern.compile(
            "([€£₹])\\s*([0-9][0-9,]*(?:\\.[0-9]{1,4})?)\\b");
    private static final Pattern PLAN = Pattern.compile(
            "(?im)\\b(?:plan|membership)\\s*:\\s*([^\\n]{1,120})$");
    private static final Pattern EXPLICIT_DATE = Pattern.compile(
            "(?i)\\b(?:renews?|renewal date|next (?:billing|payment)(?: date)?)"
                    + "\\s*(?:on|:)?\\s*"
                    + "([A-Z][a-z]{2,8}\\s+\\d{1,2},?\\s+\\d{4}|\\d{4}-\\d{2}-\\d{2})");
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM d uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d uuuu", Locale.ENGLISH)
    );

    static {
        PROVIDERS.put("netflix", "Netflix");
        PROVIDERS.put("spotify", "Spotify");
        PROVIDERS.put("adobe", "Adobe");
        PROVIDERS.put("microsoft 365", "Microsoft 365");
        PROVIDERS.put("google one", "Google One");
        PROVIDERS.put("apple", "Apple");
        PROVIDERS.put("amazon prime", "Amazon Prime");
        PROVIDERS.put("youtube premium", "YouTube Premium");
        PROVIDERS.put("disney+", "Disney+");
        PROVIDERS.put("dropbox", "Dropbox");
    }

    public SubscriptionExtraction extract(NormalizedInboundEmail email) {
        String text = email.searchableText();
        String lower = text.toLowerCase(Locale.ROOT);
        InboundEmailClassification classification = classify(lower, email.senderDomain());
        String actionUrl = classification == InboundEmailClassification.GMAIL_VERIFICATION
                ? gmailVerificationUrl(text)
                : null;
        String provider = provider(lower, email.senderDomain(), classification);
        Money money = money(text);
        Billing billing = billing(lower);
        LocalDate renewalDate = renewalDate(text);
        String planName = captured(PLAN, text);

        List<String> evidence = new ArrayList<>();
        BigDecimal confidence = BigDecimal.ZERO;
        if (provider != null) {
            confidence = confidence.add(new BigDecimal("0.3500"));
            evidence.add("provider-explicit");
        }
        if (classification != InboundEmailClassification.NOT_SUBSCRIPTION) {
            confidence = confidence.add(new BigDecimal("0.2000"));
            evidence.add("event-phrase");
        }
        if (money != null) {
            confidence = confidence.add(new BigDecimal("0.2000"));
            evidence.add("labeled-money");
        }
        if (billing != null) {
            confidence = confidence.add(new BigDecimal("0.1000"));
            evidence.add("billing-cadence");
        }
        if (renewalDate != null) {
            confidence = confidence.add(new BigDecimal("0.1000"));
            evidence.add("explicit-renewal-date");
        }
        if (planName != null) {
            confidence = confidence.add(new BigDecimal("0.0500"));
            evidence.add("labeled-plan");
        }

        return new SubscriptionExtraction(
                classification,
                provider,
                planName,
                money == null ? null : money.amount(),
                money == null ? null : money.currency(),
                billing == null ? null : billing.unit(),
                billing == null ? null : billing.count(),
                renewalDate,
                confidence.min(BigDecimal.ONE).setScale(4),
                String.join(",", evidence),
                actionUrl
        );
    }

    private InboundEmailClassification classify(String text, String senderDomain) {
        if (GMAIL_VERIFICATION_SENDERS.contains(senderDomain)
                && containsAll(text, "gmail", "forwarding", "confirmation")) {
            return InboundEmailClassification.GMAIL_VERIFICATION;
        }
        if (containsAny(text, "subscription cancelled", "subscription canceled",
                "membership cancelled", "membership canceled")) {
            return InboundEmailClassification.CANCELLATION;
        }
        if (containsAny(text, "price change", "new price", "price will change")) {
            return InboundEmailClassification.PRICE_CHANGE;
        }
        if (containsAny(text, "payment received", "payment successful", "you were charged",
                "we charged", "subscription receipt", "renewal receipt")) {
            return InboundEmailClassification.RENEWAL_PAYMENT;
        }
        if (containsAny(text, "will renew", "upcoming renewal", "next payment",
                "next billing date")) {
            return InboundEmailClassification.UPCOMING_RENEWAL;
        }
        if (containsAny(text, "subscription confirmed", "welcome to your subscription",
                "membership confirmed", "you subscribed")) {
            return InboundEmailClassification.NEW_SUBSCRIPTION;
        }
        return InboundEmailClassification.NOT_SUBSCRIPTION;
    }

    private String gmailVerificationUrl(String text) {
        Matcher matcher = HTTPS_URL.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group()
                    .replaceAll("[).,;]+$", "");
            if (candidate.length() > 2000) {
                continue;
            }
            try {
                URI uri = new URI(candidate);
                if ("https".equalsIgnoreCase(uri.getScheme())
                        && "mail-settings.google.com".equalsIgnoreCase(uri.getHost())
                        && uri.getRawPath() != null
                        && uri.getRawPath().startsWith("/mail/vf-")
                        && uri.getUserInfo() == null
                        && uri.getPort() == -1) {
                    return uri.toASCIIString();
                }
            } catch (URISyntaxException ignored) {
                // Ignore malformed and untrusted links.
            }
        }
        return null;
    }

    private String provider(String text, String senderDomain,
                            InboundEmailClassification classification) {
        for (Map.Entry<String, String> provider : PROVIDERS.entrySet()) {
            if (text.contains(provider.getKey())
                    || (senderDomain != null && senderDomain.contains(provider.getKey().split(" ")[0]))) {
                return provider.getValue();
            }
        }
        if (classification == InboundEmailClassification.GMAIL_VERIFICATION) {
            return "Gmail";
        }
        if (classification == InboundEmailClassification.NOT_SUBSCRIPTION
                || senderDomain == null || PERSONAL_EMAIL_DOMAINS.contains(senderDomain)) {
            return null;
        }
        String[] labels = senderDomain.split("\\.");
        if (labels.length < 2) {
            return null;
        }
        String label = labels[labels.length - 2].replace('-', ' ').trim();
        if (label.isEmpty()) {
            return null;
        }
        return Arrays.stream(label.split(" "))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .reduce((left, right) -> left + " " + right)
                .orElse(null);
    }

    private Money money(String text) {
        for (String line : text.split("\\n")) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (!containsAny(lower, "total", "charged", "paid", "payment", "amount", "price")) {
                continue;
            }
            Matcher coded = CODE_MONEY.matcher(line);
            if (coded.find()) {
                return new Money(decimal(coded.group(2)), coded.group(1).toUpperCase(Locale.ROOT));
            }
            Matcher reversed = REVERSED_CODE_MONEY.matcher(line);
            if (reversed.find()) {
                return new Money(decimal(reversed.group(1)),
                        reversed.group(2).toUpperCase(Locale.ROOT));
            }
            Matcher symbol = SYMBOL_MONEY.matcher(line);
            if (symbol.find()) {
                String currency = switch (symbol.group(1)) {
                    case "€" -> "EUR";
                    case "£" -> "GBP";
                    case "₹" -> "INR";
                    default -> null;
                };
                if (currency != null) {
                    return new Money(decimal(symbol.group(2)), currency);
                }
            }
        }
        return null;
    }

    private Billing billing(String text) {
        Matcher every = Pattern.compile(
                "(?i)\\bevery\\s+(\\d{1,3})\\s+(day|week|month|year)s?\\b").matcher(text);
        if (every.find()) {
            return new Billing(BillingUnit.fromValue(every.group(2)),
                    Integer.parseInt(every.group(1)));
        }
        if (containsAny(text, "per month", "monthly", "each month")) {
            return new Billing(BillingUnit.MONTH, 1);
        }
        if (containsAny(text, "per year", "yearly", "annually", "annual plan")) {
            return new Billing(BillingUnit.YEAR, 1);
        }
        if (containsAny(text, "per week", "weekly")) {
            return new Billing(BillingUnit.WEEK, 1);
        }
        return null;
    }

    private LocalDate renewalDate(String text) {
        String value = captured(EXPLICIT_DATE, text);
        if (value == null) {
            return null;
        }
        String cleaned = value.replace(",", "");
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(cleaned, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next explicit format.
            }
        }
        return null;
    }

    private String captured(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value.replace(",", ""));
    }

    private boolean containsAny(String text, String... values) {
        return Arrays.stream(values).anyMatch(text::contains);
    }

    private boolean containsAll(String text, String... values) {
        return Arrays.stream(values).allMatch(text::contains);
    }

    private record Money(BigDecimal amount, String currency) {
    }

    private record Billing(BillingUnit unit, int count) {
    }
}
