package com.track.subscription_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionLoggingDefaultsTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void verboseFrameworkAndSqlLoggingIsDisabledByDefault() {
        contextRunner.run(context -> {
            var environment = context.getEnvironment();
            assertThat(environment.getProperty("spring.jpa.show-sql", Boolean.class))
                    .isFalse();
            assertThat(environment.getProperty("logging.level.org.hibernate.SQL"))
                    .isEqualTo("WARN");
            assertThat(environment.getProperty("logging.level.org.hibernate.orm.jdbc.bind"))
                    .isEqualTo("WARN");
            assertThat(environment.getProperty("logging.level.org.springframework.security"))
                    .isEqualTo("INFO");
            assertThat(environment.getProperty("logging.level.org.springframework.web"))
                    .isEqualTo("INFO");
        });
    }

    @Test
    void operatorsCanTemporarilyEnableTargetedDiagnostics() {
        contextRunner
                .withPropertyValues(
                        "JPA_SHOW_SQL=true",
                        "HIBERNATE_SQL_LOG_LEVEL=DEBUG",
                        "HIBERNATE_BIND_LOG_LEVEL=TRACE",
                        "SPRING_SECURITY_LOG_LEVEL=DEBUG",
                        "SPRING_WEB_LOG_LEVEL=DEBUG")
                .run(context -> {
                    var environment = context.getEnvironment();
                    assertThat(environment.getProperty("spring.jpa.show-sql", Boolean.class))
                            .isTrue();
                    assertThat(environment.getProperty("logging.level.org.hibernate.SQL"))
                            .isEqualTo("DEBUG");
                    assertThat(environment.getProperty(
                            "logging.level.org.hibernate.orm.jdbc.bind"))
                            .isEqualTo("TRACE");
                    assertThat(environment.getProperty(
                            "logging.level.org.springframework.security"))
                            .isEqualTo("DEBUG");
                    assertThat(environment.getProperty("logging.level.org.springframework.web"))
                            .isEqualTo("DEBUG");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @PropertySource("classpath:application.properties")
    static class PropertiesConfiguration {
    }
}
