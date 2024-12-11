package ru.milko.student_vertx.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import ru.milko.student_vertx.config.Config;

import java.net.URL;

public class FlywayMigration {
    private static String url;
    private static String username;
    private static String password;

    public static void migrate(Config config) {
        configure(config);

        Flyway flyway = Flyway.configure()
                .dataSource(url, username, password)
//                .locations("filesystem:src/main/resources/db/migration")
                .validateMigrationNaming(true)
//                .sqlMigrationPrefix("V")
//                .sqlMigrationSeparator("__")
//                .sqlMigrationSuffixes(".sql")
                .load();

        flyway.migrate();
    }

    private static void configure(Config config) {
        url = config.get("flyway.database.url");
        username = config.get("flyway.database.username");
        password = config.get("flyway.database.password");
        validateFlywayProperties();
    }

    private static void validateFlywayProperties() {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Database host is not set or is empty.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Database username is not set or is empty.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Database password is not set or is empty.");
        }
    }
}
