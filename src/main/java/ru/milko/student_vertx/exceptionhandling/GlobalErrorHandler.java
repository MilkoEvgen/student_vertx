package ru.milko.student_vertx.exceptionhandling;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgException;
import ru.milko.student_vertx.exceptions.EntityNotFoundException;

import java.time.LocalDateTime;

public class GlobalErrorHandler {
    public static void handle(RoutingContext context) {
        System.out.println("*** in GlobalErrorHandler");
        Throwable failure = context.failure();
        int statusCode = context.statusCode() > 0 ? context.statusCode() : 500;

        if (failure instanceof PgException) {
            statusCode = 400;
        } else if (failure instanceof EntityNotFoundException) {
            statusCode = 404;
        }

        String errorType = (failure != null) ? failure.getClass().getSimpleName() : "UnknownError";
        String message = (failure != null) ? failure.getMessage() : "Unexpected error";

        JsonObject errorResponse = new JsonObject()
                .put("timestamp", LocalDateTime.now().toString())
                .put("status", statusCode)
                .put("error", errorType)
                .put("message", message)
                .put("path", context.request().path());

        context.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(errorResponse.encode());
    }
}
