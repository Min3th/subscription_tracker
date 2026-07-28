package com.track.subscription_service.notification.controller;

import com.track.subscription_service.inboundemail.config.InboundEmailProperties;
import com.track.subscription_service.inboundemail.controller.InboundEmailWebhookController;
import com.track.subscription_service.inboundemail.service.InboundEmailIngestionService;
import com.track.subscription_service.notification.service.SendGridEventService;
import com.track.subscription_service.notification.service.UnsubscribeService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SendGridWebhookConditionTest {

    @Test
    void disabledFlagRemovesWebhookControllersButKeepsUnsubscribeController() {
        context()
                .withPropertyValues("app.email.sendgrid-inbound-enabled=false")
                .run(context -> {
                    assertThat(context)
                            .doesNotHaveBean(InboundEmailWebhookController.class)
                            .doesNotHaveBean(SendGridEventWebhookController.class)
                            .hasSingleBean(NotificationLifecycleController.class);
                });
    }

    @Test
    void rollbackDefaultKeepsWebhookControllers() {
        context().run(context -> assertThat(context)
                .hasSingleBean(InboundEmailWebhookController.class)
                .hasSingleBean(SendGridEventWebhookController.class)
                .hasSingleBean(NotificationLifecycleController.class));
    }

    private ApplicationContextRunner context() {
        return new ApplicationContextRunner()
                .withBean(
                        InboundEmailIngestionService.class,
                        () -> mock(InboundEmailIngestionService.class))
                .withBean(InboundEmailProperties.class, InboundEmailProperties::new)
                .withBean(
                        SendGridEventService.class,
                        () -> mock(SendGridEventService.class))
                .withBean(
                        UnsubscribeService.class,
                        () -> mock(UnsubscribeService.class))
                .withUserConfiguration(
                        InboundEmailWebhookController.class,
                        SendGridEventWebhookController.class,
                        NotificationLifecycleController.class);
    }
}
