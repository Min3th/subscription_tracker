package com.track.subscription_service.notification.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class TemplateService {

    public String loadTemplate(String name, String date) {
        try {
            InputStream inputStream =
                    new ClassPathResource("templates/reminder.html").getInputStream();

            String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            html = html.replace("{{subscription_name}}", name);
            html = html.replace("{{renewal_date}}", date);

            return html;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load email template", e);
        }
    }

}
