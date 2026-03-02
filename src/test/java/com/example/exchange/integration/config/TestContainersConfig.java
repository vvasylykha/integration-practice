package com.example.exchange.integration.config;

import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Shared Testcontainers configuration for all integration tests.
 *
 * <p><b>Task:</b> annotate and implement this class:
 * <ul>
 *   <li>Mark the class as a Spring test-only configuration so it is not picked up
 *       during a normal application startup.</li>
 *   <li>Declare three static container fields — PostgreSQL, Redis ({@code GenericContainer}
 *       with port 6379 exposed), and RabbitMQ — using appropriate Docker images.</li>
 *   <li>Start all containers in a static initializer block so they are shared
 *       across the entire test suite (started once, not per test class).</li>
 *   <li>Annotate each {@code @Bean} method with {@code @ServiceConnection} so that
 *       Spring Boot automatically configures datasource, cache, and broker URLs
 *       from the running container. For {@code GenericContainer} (Redis), the
 *       connection type must be identified explicitly.</li>
 * </ul>
 */
public class TestContainersConfig {

    /**
     * Provides the PostgreSQL container as a Spring bean.
     * Spring Boot reads JDBC URL, username, and password directly from the container.
     */
    @Bean
    PostgreSQLContainer<?> postgresContainer() {
        // TODO: implement
        return null;
    }

    /**
     * Provides the Redis container as a Spring bean.
     * Expose port 6379 and annotate so Spring Boot configures the Redis connection.
     */
    @Bean
    GenericContainer<?> redisContainer() {
        // TODO: implement
        return null;
    }

    /**
     * Provides the RabbitMQ container as a Spring bean.
     * Spring Boot reads AMQP host, port, and credentials directly from the container.
     */
    @Bean
    RabbitMQContainer rabbitMQContainer() {
        // TODO: implement
        return null;
    }
}
