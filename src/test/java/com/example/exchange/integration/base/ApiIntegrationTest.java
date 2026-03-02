package com.example.exchange.integration.base;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Abstract base class for full-stack HTTP integration tests.
 *
 * <p><b>Task:</b> annotate this class so that:
 * <ul>
 *   <li>The full Spring application context starts on a randomly assigned port.</li>
 *   <li>The {@code "test"} Spring profile is activated.</li>
 *   <li>{@code TestContainersConfig} is imported to register PostgreSQL, Redis,
 *       and RabbitMQ containers.</li>
 * </ul>
 *
 * <p>All concrete IT classes extend this base class.
 * RestAssured must be configured for each test in the {@code @BeforeEach} method.
 */
public abstract class ApiIntegrationTest {

    @LocalServerPort
    private int port;

    /**
     * Configure RestAssured to target the randomly assigned server port
     * and enable request/response logging on assertion failures.
     */
    @BeforeEach
    void setUpRestAssured() {
        // TODO: implement
    }
}
