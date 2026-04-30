package com.track.subscription_service.notification.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.track.subscription_service.notification.config.SendGridProperties;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

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

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            System.out.println("SendGrid status: " + response.getStatusCode());
            System.out.println("SendGrid body: " + response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
