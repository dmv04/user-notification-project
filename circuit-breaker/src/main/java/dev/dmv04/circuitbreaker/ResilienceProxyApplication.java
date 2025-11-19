package dev.dmv04.circuitbreaker;

import dev.dmv04.circuitbreaker.config.ServiceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ServiceConfiguration.class)
@ConfigurationPropertiesScan
public class ResilienceProxyApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResilienceProxyApplication.class, args);
    }
}
