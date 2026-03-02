package com.example.exchange.integration.migration;

import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;

/**
 * Integration tests verifying Flyway migration state after application startup.
 *
 * <p><b>Task:</b> annotate this class to start the Spring context without a web server,
 * activate the {@code "test"} profile, and import {@code TestContainersConfig}.
 *
 * <p>Also inject the {@link org.flywaydb.core.Flyway} bean to inspect migration metadata.
 */
class FlywayMigrationIT {

    /**
     * There must be at least one applied migration and every applied migration
     * must have the state {@link MigrationState#SUCCESS}.
     */
    @Test
    void shouldApplyAllMigrationsSuccessfully() {
        // TODO: implement
    }

    /**
     * There should be no pending (unapplied) migrations after context startup —
     * all migration scripts on the classpath must already be applied.
     */
    @Test
    void shouldHaveNoPendingMigrations() {
        // TODO: implement
    }

    /**
     * None of the migrations known to Flyway should be in a {@link MigrationState#FAILED} state.
     */
    @Test
    void shouldNotHaveFailedMigrations() {
        // TODO: implement
    }
}
