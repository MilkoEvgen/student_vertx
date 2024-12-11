package ru.milko.student_vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Pool;
import ru.milko.student_vertx.config.Config;
import ru.milko.student_vertx.config.DatabasePoolConfig;
import ru.milko.student_vertx.database.FlywayMigration;

public class MyVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
        String activeProfile = System.getProperty("profile", "default");
        Router router = Router.router(vertx);
        Config config = new Config("src/main/resources/application-" + activeProfile + ".properties");
        FlywayMigration.migrate(config);
        Pool pool = DatabasePoolConfig.createPool(vertx, config);

        ApplicationContext context = new ApplicationContext(pool);
        context.initDependencies();
        context.registerRoutes(router);

        int port = Integer.parseInt(config.get("http.port"));

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port, http -> {
                    if (http.succeeded()) {
                        System.out.println("HTTP сервер запущен на порту " + port);
                        startPromise.complete();
                    } else {
                        System.err.println("Не удалось запустить HTTP сервер: " + http.cause().getMessage());
                        startPromise.fail(http.cause());
                    }
                });
    }
}