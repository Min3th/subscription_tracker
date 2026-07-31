package com.track.subscription_service.inboundemail.service;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.dto.ParsedInboundEmail;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.core.FileItemInputIterator;
import org.apache.commons.fileupload2.core.RequestContext;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletDiskFileUpload;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class SendGridInboundMultipartParser {
    private static final Set<String> ACCEPTED_FIELDS = Set.of(
            "envelope", "from", "to", "subject", "text", "html", "headers", "spam_score"
    );

    private final InboundEmailProperties properties;

    public SendGridInboundMultipartParser(InboundEmailProperties properties) {
        this.properties = properties;
    }

    public ParsedInboundEmail parse(byte[] rawBody, String contentType) {
        if (rawBody == null || rawBody.length == 0) {
            throw new IllegalArgumentException("Inbound email request body is empty");
        }
        if (rawBody.length > properties.getMaxRequestBytes()) {
            throw new IllegalArgumentException("Inbound email request exceeds the size limit");
        }
        if (contentType == null
                || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/form-data")) {
            throw new IllegalArgumentException("Inbound email request must be multipart/form-data");
        }

        JakartaServletDiskFileUpload upload = new JakartaServletDiskFileUpload();
        upload.setMaxSize(properties.getMaxRequestBytes());
        upload.setMaxFileSize(properties.getMaxRequestBytes());
        upload.setMaxFileCount(properties.getMaxParts());
        upload.setMaxPartHeaderSize(8192);
        upload.setHeaderCharset(StandardCharsets.UTF_8);

        Map<String, String> fields = new HashMap<>();
        int attachments = 0;
        int parts = 0;
        try {
            FileItemInputIterator iterator = upload.getItemIterator(
                    requestContext(rawBody, contentType));
            while (iterator.hasNext()) {
                FileItemInput item = iterator.next();
                parts++;
                if (parts > properties.getMaxParts()) {
                    throw new IllegalArgumentException("Inbound email request has too many parts");
                }
                if (!item.isFormField()) {
                    attachments++;
                    continue;
                }
                String fieldName = item.getFieldName();
                if (!ACCEPTED_FIELDS.contains(fieldName)) {
                    continue;
                }
                if (fields.containsKey(fieldName)) {
                    throw new IllegalArgumentException("Inbound email request contains duplicate fields");
                }
                fields.put(fieldName, readField(item.getInputStream()));
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid inbound email multipart payload", exception);
        }

        return new ParsedInboundEmail(
                fields.get("envelope"),
                fields.get("from"),
                fields.get("to"),
                fields.get("subject"),
                fields.get("text"),
                fields.get("html"),
                fields.get("headers"),
                decimal(fields.get("spam_score")),
                attachments
        );
    }

    private String readField(InputStream input) throws IOException {
        byte[] value = input.readNBytes(properties.getMaxFieldBytes() + 1);
        if (value.length > properties.getMaxFieldBytes()) {
            throw new IllegalArgumentException("Inbound email field exceeds the size limit");
        }
        return new String(value, StandardCharsets.UTF_8);
    }

    private BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Inbound email spam score is invalid", exception);
        }
    }

    private RequestContext requestContext(byte[] rawBody, String contentType) {
        return new RequestContext() {
            @Override
            public String getCharacterEncoding() {
                return StandardCharsets.UTF_8.name();
            }

            @Override
            public long getContentLength() {
                return rawBody.length;
            }

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(rawBody);
            }

            @Override
            public boolean isMultipartRelated() {
                return false;
            }
        };
    }
}
