package ru.milko.student_vertx.rest;

import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public abstract class BasicController {
    public abstract void registerRoutes(Router router);

    protected void respondSuccess(RoutingContext context, int statusCode, Object body) {
        context.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(Json.encodePrettily(body));
    }

    protected void respondError(RoutingContext context, int statusCode, String message) {
        context.response()
                .setStatusCode(statusCode)
                .end(message);
    }

    protected <T> T parseRequestBody(RoutingContext context, Class<T> clazz) {
        T t;
        try {
            t = Json.decodeValue(context.body().asString(), clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid request body: " + e.getMessage(), e);
        }
        return t;
    }
}
