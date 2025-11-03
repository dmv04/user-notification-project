package dev.dmv04.userservice.config;

import dev.dmv04.userservice.producer.UserEventProducer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public UserEventProducer userEventProducer() {
        return mock(UserEventProducer.class);
    }
}
