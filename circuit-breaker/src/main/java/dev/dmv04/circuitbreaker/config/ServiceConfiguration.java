package dev.dmv04.circuitbreaker.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "services")
public class ServiceConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ServiceConfiguration.class);
    private Map<String, ServiceConfig> services = new HashMap<>();

    public ServiceConfiguration(Map<String, ServiceConfig> services) {
        this.services = services;
    }

    public Map<String, ServiceConfig> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceConfig> services) {
        this.services = services;
    }

    public static class ServiceConfig {

        public ServiceConfig() {
        }

        public ServiceConfig(String url) {
            this.url = url;
        }

        public ServiceConfig(String url, Integer timeout) {
            this.url = url;
            this.timeout = timeout;
        }

        private String url;

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        private int timeout;


        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    @PostConstruct
    public void logServices() {
        logger.warn("LOADED SERVICES CONFIGURATION");
        logger.warn("Total services: " + services.size());
        services.forEach((key, value) -> {
            logger.warn("Service: {} -> {}", key, value);
        });
    }
}
