package ru.milko.student_vertx.integration;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.milko.student_vertx.MyVerticle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@ExtendWith(VertxExtension.class)
public class TeacherControllerTest extends BaseIntegrationTest {
    private static Vertx vertx;
    private static WebClient webClient;
    private static int serverPort;
    private static final String serverHost = "localhost";
    private static final String serverUrl = "/api/v1/teachers";

    private final JsonObject teacher = new JsonObject()
            .put("name", "John Doe");

    @BeforeAll
    static void startApplication(VertxTestContext testContext) {
        vertx = Vertx.vertx();

        vertx.deployVerticle(new MyVerticle(), testContext.succeeding(id -> {
            webClient = WebClient.create(vertx);
            serverPort = Integer.parseInt(System.getProperty("actual.port", "0"));
            testContext.completeNow();
        }));
    }

    @AfterEach
    void cleanDatabase(VertxTestContext testContext) {
        Future<Void> deleteFuture = Future.future(promise -> {
            try (Connection connection = DriverManager.getConnection(
                    System.getProperty("flyway.database.url"),
                    System.getProperty("flyway.database.username"),
                    System.getProperty("flyway.database.password"))) {
                Statement stmt = connection.createStatement();
                stmt.executeUpdate("DELETE FROM teachers");
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        });
        deleteFuture
                .onSuccess(result -> testContext.completeNow())
                .onFailure(testContext::failNow);
    }

    @AfterAll
    static void stopApplication(VertxTestContext testContext) {
        if (vertx != null) {
            vertx.close(testContext.succeedingThenComplete());
        }
    }

    @Test
    void createTeacherShouldReturnCreatedTeacher(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(teacher, testContext.succeeding(response -> testContext.verify(() -> {
                    Assertions.assertEquals(201, response.statusCode());
                    JsonObject createdTeacher = response.bodyAsJsonObject();
                    Assertions.assertNotNull(createdTeacher.getLong("id"));
                    Assertions.assertEquals("John Doe", createdTeacher.getString("name"));
                    testContext.completeNow();
                })));
    }

    @Test
    void findAllTeachersShouldReturnListOfTeachers(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(teacher, testContext.succeeding(createResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createResponse.statusCode());

                    webClient.get(serverPort, serverHost, serverUrl)
                            .send(testContext.succeeding(findAllResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(200, findAllResponse.statusCode());

                                JsonArray teachers = findAllResponse.bodyAsJsonArray();
                                Assertions.assertEquals(1, teachers.size());

                                JsonObject returnedTeacher = teachers.getJsonObject(0);
                                Assertions.assertNotNull(returnedTeacher.getLong("id"));
                                Assertions.assertEquals("John Doe", returnedTeacher.getString("name"));
                                testContext.completeNow();
                            })));
                })));
    }

    @Test
    void findAllTeachersShouldReturnEmptyList(VertxTestContext testContext) {
        webClient.get(serverPort, serverHost, serverUrl)
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    Assertions.assertEquals(200, response.statusCode());

                    JsonArray teachers = response.bodyAsJsonArray();
                    Assertions.assertTrue(teachers.isEmpty());

                    testContext.completeNow();
                })));
    }

    @Test
    void findTeacherByIdShouldReturnTeacher(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(teacher, testContext.succeeding(createResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createResponse.statusCode());
                    JsonObject createdTeacher = createResponse.bodyAsJsonObject();
                    Long id = createdTeacher.getLong("id");

                    webClient.get(serverPort, serverHost, serverUrl + "/" + id)
                            .send(testContext.succeeding(returnedResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(200, returnedResponse.statusCode());

                                JsonObject returnedTeacher = returnedResponse.bodyAsJsonObject();
                                Assertions.assertEquals(id, returnedTeacher.getLong("id"));
                                Assertions.assertEquals("John Doe", returnedTeacher.getString("name"));
                                testContext.completeNow();
                            })));
                })));
    }

    @Test
    void findTeacherByIdShouldThrowException(VertxTestContext testContext) {
        long wrongId = 999L;

        webClient.get(serverPort, serverHost, serverUrl + "/" + wrongId)
                .send(testContext.succeeding(errorResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(404, errorResponse.statusCode());

                    JsonObject errorBody = errorResponse.bodyAsJsonObject();
                    Assertions.assertNotNull(errorBody.getString("timestamp"));
                    Assertions.assertEquals(404, errorBody.getInteger("status"));
                    Assertions.assertEquals("EntityNotFoundException", errorBody.getString("error"));
                    Assertions.assertEquals("Teacher with ID " + wrongId + " not found", errorBody.getString("message"));
                    testContext.completeNow();
                })));
    }

    @Test
    void updateTeacherShouldReturnUpdatedTeacher(VertxTestContext testContext) {
        JsonObject updatedTeacher = new JsonObject().put("name", "Updated Teacher");

        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(teacher, testContext.succeeding(createResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createResponse.statusCode());
                    JsonObject createdTeacher = createResponse.bodyAsJsonObject();
                    Long id = createdTeacher.getLong("id");

                    webClient.patch(serverPort, serverHost, serverUrl + "/" + id)
                            .sendJson(updatedTeacher, testContext.succeeding(updateResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(200, updateResponse.statusCode());

                                JsonObject returnedTeacher = updateResponse.bodyAsJsonObject();
                                Assertions.assertEquals(id, returnedTeacher.getLong("id"));
                                Assertions.assertEquals("Updated Teacher", returnedTeacher.getString("name"));

                                testContext.completeNow();
                            })));
                })));
    }

    @Test
    void updateTeacherShouldThrowException(VertxTestContext testContext) {
        long wrongId = 999L;
        JsonObject updatedTeacher = new JsonObject().put("name", "Updated Teacher");

        webClient.patch(serverPort, serverHost, serverUrl + "/" + wrongId)
                .sendJson(updatedTeacher, testContext.succeeding(response -> testContext.verify(() -> {
                    Assertions.assertEquals(404, response.statusCode());

                    JsonObject errorBody = response.bodyAsJsonObject();
                    Assertions.assertNotNull(errorBody.getString("timestamp"));
                    Assertions.assertEquals(404, errorBody.getInteger("status"));
                    Assertions.assertEquals("EntityNotFoundException", errorBody.getString("error"));
                    Assertions.assertEquals("Teacher with ID " + wrongId + " not found", errorBody.getString("message"));

                    testContext.completeNow();
                })));
    }

    @Test
    void deleteTeacherShouldRemoveEntityFromDatabase(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(teacher, testContext.succeeding(createResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createResponse.statusCode());
                    JsonObject createdTeacher = createResponse.bodyAsJsonObject();
                    Long id = createdTeacher.getLong("id");

                    webClient.delete(serverPort, serverHost, serverUrl + "/" + id)
                            .send(testContext.succeeding(deleteResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(204, deleteResponse.statusCode());

                                webClient.get(serverPort, serverHost, serverUrl + "/" + id)
                                        .send(testContext.succeeding(getResponse -> testContext.verify(() -> {
                                            Assertions.assertEquals(404, getResponse.statusCode());
                                            testContext.completeNow();
                                        })));
                            })));
                })));
    }

    @Test
    void deleteTeacherShouldReturnNoContentIfTeacherNotExists(VertxTestContext testContext) {
        long wrongTeacherId = 999L;

        webClient.delete(serverPort, serverHost, serverUrl + "/" + wrongTeacherId)
                .send(testContext.succeeding(deleteResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(204, deleteResponse.statusCode());
                    testContext.completeNow();
                })));
    }
}
