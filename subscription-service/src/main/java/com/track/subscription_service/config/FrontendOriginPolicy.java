package com.track.subscription_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@Component
public class FrontendOriginPolicy {

    private final List<String> allowedOrigins;

    public FrontendOriginPolicy(@Value("${app.frontend.origins}") String configuredOrigins) {
        this.allowedOrigins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .map(this::withoutTrailingSlash)
                .filter(origin -> !origin.isBlank())
                .distinct()
                .toList();

        if (allowedOrigins.isEmpty()) {
            throw new IllegalStateException("At least one frontend origin must be configured");
        }
    }

    public List<String> allowedOrigins() {
        return allowedOrigins;
    }

    public void requireAllowedBrowserOrigin(String origin) {
        // Non-browser clients may omit Origin. Browser cross-origin requests include it.
        if (origin != null && !allowedOrigins.contains(withoutTrailingSlash(origin))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Origin is not allowed");
        }
    }

    private String withoutTrailingSlash(String origin) {
        return origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
    }
}
