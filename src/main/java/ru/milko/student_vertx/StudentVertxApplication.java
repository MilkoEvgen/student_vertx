package ru.milko.student_vertx;


import io.vertx.core.Vertx;

public class StudentVertxApplication {
	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx();

		vertx.deployVerticle(new MyVerticle(), result -> {
			if (result.succeeded()) {
				System.out.println("StudentVertx успешно развернут!");
			} else {
				System.err.println("Не удалось развернуть StudentVertx: " + result.cause());
			}
		});
	}
}
