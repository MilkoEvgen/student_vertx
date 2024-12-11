package ru.milko.student_vertx.config;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

public class DatabasePoolConfig {
    private static int port;
    private static String host;
    private static String database;
    private static String username;
    private static String password;
    private static int maxSize;

    public static Pool createPool(Vertx vertx, Config config) {
        configure(config);
        validateDatabaseProperties();

        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(port)
                .setHost(host)
                .setDatabase(database)
                .setUser(username)
                .setPassword(password);

        PoolOptions poolOptions = new PoolOptions().setMaxSize(maxSize);

        return Pool.pool(vertx, connectOptions, poolOptions);
    }

    private static void configure(Config config) {
        port = Integer.parseInt(config.get("database.port"));
        host = config.get("database.host");
        database = config.get("database.name");
        username = config.get("database.username");
        password = config.get("database.password");
        maxSize = Integer.parseInt(config.get("database.pool.maxsize"));
    }

    private static void validateDatabaseProperties() {
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("Database host is not set or is empty.");
        }
        if (database == null || database.isBlank()) {
            throw new IllegalStateException("Database name is not set or is empty.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Database username is not set or is empty.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Database password is not set or is empty.");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalStateException("Database port is invalid. It must be between 1 and 65535.");
        }
        if (maxSize <= 0) {
            throw new IllegalStateException("Database pool max size must be greater than 0.");
        }
    }
}
