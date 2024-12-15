package ru.milko.student_vertx.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;

public class BaseIntegrationTest {
    protected static PostgreSQLContainer<?> postgres;

    @BeforeAll
    static void startTestContainer() {
        postgres = new PostgreSQLContainer<>("postgres:16.0")
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass");
        postgres.start();

        System.setProperty("http.port", "0");

        System.setProperty("flyway.database.url", postgres.getJdbcUrl());
        System.setProperty("flyway.database.username", postgres.getUsername());
        System.setProperty("flyway.database.password", postgres.getPassword());

        System.setProperty("database.host", postgres.getHost());
        System.setProperty("database.port", String.valueOf(postgres.getFirstMappedPort()));
        System.setProperty("database.name", postgres.getDatabaseName());
        System.setProperty("database.username", postgres.getUsername());
        System.setProperty("database.password", postgres.getPassword());
    }

    @AfterAll
    static void stopTestContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }
}
