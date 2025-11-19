package dev.dmv04.circuitbreaker.controller;

import dev.dmv04.circuitbreaker.config.ServiceConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RestController
public class ProxyController {

    private final WebClient webClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ServiceConfiguration serviceProperties;
    private final Map<String, CircuitBreaker> circuitBreakerCache;
    private static final Logger logger = LoggerFactory.getLogger(ProxyController.class);

    public ProxyController(WebClient.Builder webClientBuilder,
                           CircuitBreakerRegistry circuitBreakerRegistry,
                           ServiceConfiguration serviceProperties) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.serviceProperties = serviceProperties;
        this.circuitBreakerCache = new ConcurrentHashMap<>();
    }

    @RequestMapping("/proxy/{serviceId}/**")
    public Mono<ResponseEntity<String>> proxy(
            @PathVariable String serviceId,
            ServerWebExchange exchange) {

        var config = serviceProperties.getServices().get(serviceId);
        if (config == null) {
            logger.warn("Unknown service: {}", serviceId);
            return Mono.just(ResponseEntity.badRequest()
                    .body("{\"error\": \"Unknown service: " + serviceId + "\"}"));
        }

        String baseUrl = config.getUrl();
        String path = exchange.getRequest().getPath().value().replaceFirst("/proxy/" + serviceId, "");
        if (path.isEmpty()) path = "/";
        String query = exchange.getRequest().getURI().getRawQuery();
        String targetUrl = baseUrl + path + (query != null ? "?" + query : "");

        HttpMethod method = exchange.getRequest().getMethod();
        if (method == null) method = HttpMethod.GET;

        logger.info("Proxying {} {} to: {}", method, path, targetUrl);

        CircuitBreaker circuitBreaker = circuitBreakerCache.computeIfAbsent(
                serviceId,
                circuitBreakerRegistry::circuitBreaker
        );

        logger.info("📊 Circuit Breaker state for {}: {}", serviceId, circuitBreaker.getState());
        logger.info("📊 Circuit Breaker metrics - buffered: {}, failed: {}",
                circuitBreaker.getMetrics().getNumberOfBufferedCalls(),
                circuitBreaker.getMetrics().getNumberOfFailedCalls());

        HttpMethod finalMethod = method;

        return Mono.fromCallable(() -> {
            if (circuitBreaker.getState() == CircuitBreaker.State.CLOSED ||
                    circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN) {

                try {
                    logger.info("Making REAL request (CB CLOSED/HALF_OPEN) to: {}", targetUrl);
                    ResponseEntity<String> response = makeRequestBlocking(finalMethod, targetUrl, exchange);

                    logger.info("Request completed with status: {}", response.getStatusCode());
                    return response;

                } catch (Exception e) {
                    circuitBreaker.onError(System.nanoTime(), TimeUnit.NANOSECONDS, e);
                    logger.info("📈 Registered exception in Circuit Breaker: {}", e.getMessage());

                    throw e;
                }
            }
            else if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
                logger.warn("CB FALLBACK (OPEN state) for {}: CircuitBreaker is OPEN", serviceId);
                return ResponseEntity.status(503)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(String.format(
                                "{\"error\":\"Service unavailable\",\"service\":\"%s\",\"fallback\":true}",
                                serviceId
                        ));
            }

            return makeRequestBlocking(finalMethod, targetUrl, exchange);

        }).subscribeOn(Schedulers.boundedElastic());
    }

    private ResponseEntity<String> makeRequestBlocking(HttpMethod method, String targetUrl, ServerWebExchange exchange) {
        try {
            var spec = webClient.method(method)
                    .uri(URI.create(targetUrl))
                    .headers(headers -> {
                        exchange.getRequest().getHeaders().forEach((name, values) -> {
                            if (!"host".equalsIgnoreCase(name)) {
                                headers.addAll(name, values);
                            }
                        });
                    });

            if (hasRequestBody(method)) {
                byte[] bodyBytes = exchange.getRequest().getBody()
                        .reduce(new byte[0], (acc, buffer) -> {
                            byte[] next = new byte[acc.length + buffer.readableByteCount()];
                            System.arraycopy(acc, 0, next, 0, acc.length);
                            buffer.read(next);
                            DataBufferUtils.release(buffer);
                            return next;
                        })
                        .block();

                if (bodyBytes != null && bodyBytes.length > 0) {
                    return spec.bodyValue(bodyBytes)
                            .retrieve()
                            .toEntity(String.class)
                            .block();
                }
            }

            return spec.retrieve().toEntity(String.class).block();

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                return ResponseEntity.status(e.getStatusCode())
                        .headers(e.getHeaders())
                        .body(e.getResponseBodyAsString());
            }
            throw new RuntimeException("Service error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new RuntimeException("Connection failed", e);
        }
    }

    private boolean hasRequestBody(HttpMethod method) {
        return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
    }
}