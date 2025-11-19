package dev.dmv04.circuitbreaker.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableConfigurationProperties(ServiceConfiguration.class)
@TestPropertySource(properties = {
        "services.services.user-service.url=http://localhost:8081",
        "services.services.user-service.timeout=5000",
        "services.services.notification-service.url=http://localhost:8082",
        "services.services.notification-service.timeout=3000"
})
class ServiceConfigurationTest {

    @Autowired
    private ServiceConfiguration serviceConfiguration;

    @Test
    void shouldLoadServiceConfigurationFromProperties() {
        Map<String, ServiceConfiguration.ServiceConfig> services = serviceConfiguration.getServices();

        assertThat(services).hasSize(2);
        assertThat(services).containsKeys("user-service", "notification-service");

        ServiceConfiguration.ServiceConfig userService = services.get("user-service");
        assertThat(userService.getUrl()).isEqualTo("http://localhost:8081");
        assertThat(userService.getTimeout()).isEqualTo(5000);

        ServiceConfiguration.ServiceConfig notificationService = services.get("notification-service");
        assertThat(notificationService.getUrl()).isEqualTo("http://localhost:8082");
        assertThat(notificationService.getTimeout()).isEqualTo(3000);
    }

    @Test
    void shouldCreateServiceConfigWithAllFields() {
        ServiceConfiguration.ServiceConfig config = new ServiceConfiguration.ServiceConfig();

        config.setUrl("http://test.com");
        config.setTimeout(10000);

        assertThat(config.getUrl()).isEqualTo("http://test.com");
        assertThat(config.getTimeout()).isEqualTo(10000);
    }

    @Test
    void shouldCreateServiceConfigWithConstructor() {
        ServiceConfiguration.ServiceConfig config = new ServiceConfiguration.ServiceConfig("http://test.com", 5000);

        assertThat(config.getUrl()).isEqualTo("http://test.com");
        assertThat(config.getTimeout()).isEqualTo(5000);
    }

    @Test
    void serviceConfigToStringShouldContainUrl() {
        ServiceConfiguration.ServiceConfig config = new ServiceConfiguration.ServiceConfig("http://test.com", 5000);

        String toString = config.toString();

        assertThat(toString).isNotNull();
        assertThat(toString).isNotEmpty();
    }
}
