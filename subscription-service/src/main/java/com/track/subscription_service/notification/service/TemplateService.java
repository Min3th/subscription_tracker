package com.track.subscription_service.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class TemplateService {

    private final String frontendBaseUrl;

    public TemplateService(
            @Value("${app.frontend.base-url}") String frontendBaseUrl
    ) {
        // Prevent URLs such as https://example.com//dashboard
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    public String loadTemplate(String name, String date, String unsubscribeUrl) {

        String dashboardUrl = frontendBaseUrl + "/dashboard";

        try {
            InputStream inputStream =
                    new ClassPathResource("templates/reminder.html").getInputStream();

            String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            html = html.replace("{{subscription_name}}", name);
            html = html.replace("{{renewal_date}}", date);
            html = html.replace("{{frontend_url}}", frontendBaseUrl);
            html = html.replace("{{dashboard_url}}", dashboardUrl);
            html = html.replace("{{unsubscribe_url}}", unsubscribeUrl);

            return html;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load email template", e);
        }
    }

}
