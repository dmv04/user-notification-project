package dev.dmv04.circuitbreaker.controller;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.dmv04.circuitbreaker.config.ServiceConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProxyControllerIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private ServiceConfiguration serviceConfiguration;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("services.services.user-service.url",
                () -> "http://localhost:" + wireMockServer.getPort());
        registry.add("services.services.notification-service.url",
                () -> "http://localhost:" + wireMockServer.getPort());
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    }

    @Test
    void shouldProxyGetRequestSuccessfully() {
        wireMockServer.stubFor(get(urlEqualTo("/api/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"John Doe\"}")));

        webTestClient.get()
                .uri("/proxy/user-service/api/users/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("John Doe");

        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/users/1")));
    }

    @Test
    void shouldProxyPostRequestWithBody() {
        String requestBody = "{\"name\": \"New User\"}";

        wireMockServer.stubFor(post(urlEqualTo("/api/users"))
                .withRequestBody(equalToJson(requestBody))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody("{\"id\": 123}")));

        webTestClient.post()
                .uri("/proxy/user-service/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated();

        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/users")));
    }

    @Test
    void shouldHandle4xxErrorsFromTargetService() {
        wireMockServer.stubFor(get(urlEqualTo("/api/not-found"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Not Found")));

        webTestClient.get()
                .uri("/proxy/user-service/api/not-found")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturnReal5xxErrorWhenCircuitBreakerClosed() {
        wireMockServer.stubFor(get(urlEqualTo("/api/error"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("{\"error\": \"Internal Server Error\"}")));

        webTestClient.get()
                .uri("/proxy/user-service/api/error")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody()
                .jsonPath("$.error").isEqualTo("Internal Server Error");
    }

    @Test
    void shouldReturnFallbackWhenCircuitBreakerOpen() {
        wireMockServer.stubFor(get(urlEqualTo("/api/error"))
                .willReturn(aResponse()
                        .withStatus(500)));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("user-service");
        circuitBreaker.transitionToOpenState();

        webTestClient.get()
                .uri("/proxy/user-service/api/error")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.fallback").isEqualTo(true)
                .jsonPath("$.service").isEqualTo("user-service");
    }

    @Test
    void shouldOpenCircuitBreakerAfterMultiple5xxErrors() {
        wireMockServer.stubFor(get(urlEqualTo("/api/server-error"))
                .willReturn(aResponse()
                        .withStatus(500)));

        for (int i = 0; i < 5; i++) {
            webTestClient.get()
                    .uri("/proxy/user-service/api/server-error")
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        webTestClient.get()
                .uri("/proxy/user-service/api/server-error")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.fallback").isEqualTo(true);
    }

    @Test
    void shouldReturn400ForUnknownService() {
        webTestClient.get()
                .uri("/proxy/unknown-service/api/test")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Unknown service: unknown-service");
    }

    @Test
    void shouldProxyToDifferentServices() {
        wireMockServer.stubFor(get(urlEqualTo("/api/data"))
                .willReturn(aResponse().withStatus(200)));

        webTestClient.get()
                .uri("/proxy/user-service/api/data")
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/proxy/notification-service/api/data")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldProxyRequestWithQueryParameters() {
        wireMockServer.stubFor(get(urlPathEqualTo("/api/search"))
                .withQueryParam("q", equalTo("test"))
                .willReturn(aResponse().withStatus(200)));

        webTestClient.get()
                .uri("/proxy/user-service/api/search?q=test")
                .exchange()
                .expectStatus().isOk();

        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/api/search"))
                .withQueryParam("q", equalTo("test")));
    }

    @Test
    void shouldHandleConnectionErrorsWithFallback() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("user-service");
        circuitBreaker.transitionToOpenState();

        webTestClient.get()
                .uri("/proxy/user-service/api/non-existent")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.fallback").isEqualTo(true);
    }

    @Test
    void shouldHandleForcedOpenState() {
        wireMockServer.stubFor(get(urlEqualTo("/api/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"id\": 1}")));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("user-service");

        try {
            circuitBreaker.transitionToForcedOpenState();
        } catch (Exception e) {
            circuitBreaker.transitionToOpenState();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        webTestClient.get()
                .uri("/proxy/user-service/api/users/1")
                .exchange()
                .expectStatus().isOk(); // Должен использовать последний return

        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/users/1")));
    }

    @Test
    void shouldHandleConnectionRefusedException() {
        String invalidService = "invalid-service";

        try {
            var field = serviceConfiguration.getClass().getDeclaredField("services");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ServiceConfiguration.ServiceConfig> services =
                    (Map<String, ServiceConfiguration.ServiceConfig>) field.get(serviceConfiguration);

            ServiceConfiguration.ServiceConfig invalidConfig =
                    new ServiceConfiguration.ServiceConfig("http://localhost:9999", 5000);
            services.put(invalidService, invalidConfig);

            webTestClient.get()
                    .uri("/proxy/" + invalidService + "/api/users/1")
                    .exchange()
                    .expectStatus().is5xxServerError();

        } catch (Exception e) {
            webTestClient.get()
                    .uri("/proxy/user-service/api/invalid-path-that-causes-connection-error")
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }

    @Test
    void shouldHandleAllHttpMethods() {
        // Тест для проверки всех стандартных HTTP методов
        HttpMethod[] methodsWithBody = {HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH};
        HttpMethod[] methodsWithoutBody = {
                HttpMethod.GET, HttpMethod.DELETE, HttpMethod.HEAD,
                HttpMethod.OPTIONS, HttpMethod.TRACE
        };

        for (HttpMethod method : methodsWithBody) {
            wireMockServer.stubFor(any(urlEqualTo("/api/test"))
                    .willReturn(aResponse().withStatus(200)));

            WebTestClient.RequestBodySpec requestSpec = webTestClient.method(method)
                    .uri("/proxy/user-service/api/test")
                    .contentType(MediaType.APPLICATION_JSON);

            if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
                requestSpec.bodyValue("{\"test\": \"data\"}");
            }

            requestSpec.exchange()
                    .expectStatus().isOk();

            wireMockServer.resetAll();
        }

        for (HttpMethod method : methodsWithoutBody) {
            wireMockServer.stubFor(any(urlEqualTo("/api/test"))
                    .willReturn(aResponse().withStatus(200)));

            webTestClient.method(method)
                    .uri("/proxy/user-service/api/test")
                    .exchange()
                    .expectStatus().isOk();

            wireMockServer.resetAll();
        }
    }
}
