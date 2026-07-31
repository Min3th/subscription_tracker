package com.track.subscription_service.notification.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.track.subscription_service.notification.config.SendGridProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(
        name = "app.email.outbound-provider",
        havingValue = "sendgrid",
        matchIfMissing = true)
public class SendGridOutboundEmailSender implements OutboundEmailSender {
    private final SendGrid sendGrid;
    private final SendGridProperties properties;

    public SendGridOutboundEmailSender(
            SendGrid sendGrid,
            SendGridProperties properties) {
        this.sendGrid = sendGrid;
        this.properties = properties;
    }

    @Override
    public String send(OutboundEmailMessage message) {
        Email from = new Email(properties.getFromEmail(), properties.getFromName());
        Mail mail = new Mail(
                from,
                message.subject(),
                new Email(message.to()),
                new Content("text/html", message.html()));
        mail.addHeader("List-Unsubscribe", "<" + message.unsubscribeUrl() + ">");
        mail.addHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        try {
            request.setBody(mail.build());
            Response response = sendGrid.api(request);
            int statusCode = response.getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw EmailDeliveryException.providerRejected(
                        statusCode, statusCode == 429 || statusCode >= 500);
            }
            return response.getHeaders() == null
                    ? null
                    : response.getHeaders().get("X-Message-Id");
        } catch (IOException exception) {
            throw EmailDeliveryException.transportFailure(exception);
        }
    }
}
