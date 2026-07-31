package com.track.subscription_service.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class SesPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void defaultsToMumbaiWhenNeitherSesNorDeploymentRegionIsConfigured() {
        contextRunner.run(context -> assertThat(
                context.getBean(SesProperties.class).getRegion())
                .isEqualTo("ap-south-1"));
    }

    @Test
    void explicitSesRegionOverridesDeploymentRegion() {
        contextRunner
                .withPropertyValues(
                        "AWS_REGION=eu-west-1",
                        "SES_REGION=ap-south-1")
                .run(context -> assertThat(
                        context.getBean(SesProperties.class).getRegion())
                        .isEqualTo("ap-south-1"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SesProperties.class)
    @PropertySource("classpath:application.properties")
    static class PropertiesConfiguration {
    }
}
