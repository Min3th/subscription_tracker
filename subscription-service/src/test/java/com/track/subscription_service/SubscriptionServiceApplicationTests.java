package com.track.subscription_service;

import com.track.subscription_service.integration.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"jwt.secret=test-jwt-secret-key-that-is-longer-than-32-bytes",
		"google.client.id=test-google-client-id",
		"app.sendgrid.apiKey=SG.test",
		"app.sendgrid.fromEmail=test@example.com",
		"app.sendgrid.fromName=Subtrak Tests",
		"app.sendgrid.eventWebhookPublicKey="
})
class SubscriptionServiceApplicationTests extends PostgresIntegrationTest {

	@Test
	void contextLoads() {
	}

}
