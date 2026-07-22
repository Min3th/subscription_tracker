package com.track.subscription_service.notification.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.track.subscription_service.notification.config.SendGridProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final SendGrid sendGrid;
    private final SendGridProperties props;

    public EmailService(SendGrid sendGrid, SendGridProperties props) {
        this.sendGrid = sendGrid;
        this.props = props;
    }

    public void sendEmail(String to, String subject, String html) {

        Email from = new Email(props.getFromEmail(), props.getFromName());
        Email toEmail = new Email(to);
        Content content = new Content("text/html", html);

        Mail mail = new Mail(from, subject, toEmail, content);

        Request request = new Request();

        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");

        try {
            request.setBody(mail.build());
            Response response = sendGrid.api(request);
            int statusCode = response.getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw EmailDeliveryException.providerRejected(statusCode);
            }
            log.debug("SendGrid accepted email with status {}", statusCode);
        } catch (IOException exception) {
            throw EmailDeliveryException.transportFailure(exception);
        }
    }
}
