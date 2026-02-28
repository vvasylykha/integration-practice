package com.example.exchange.integration.migration;

import com.example.exchange.integration.config.TestContainersConfig;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class FlywayMigrationIT {

    @Autowired
    Flyway flyway;

    @Test
    void shouldApplyAllMigrationsSuccessfully() {
        MigrationInfo[] applied = flyway.info().applied();

        assertThat(applied)
                .isNotEmpty()
                .allMatch(m -> m.getState() == MigrationState.SUCCESS);
    }

    @Test
    void shouldHaveNoPendingMigrations() {
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void shouldNotHaveFailedMigrations() {
        MigrationInfo[] all = flyway.info().all();

        assertThat(Arrays.stream(all))
                .noneMatch(m -> m.getState() == MigrationState.FAILED);
    }
}
