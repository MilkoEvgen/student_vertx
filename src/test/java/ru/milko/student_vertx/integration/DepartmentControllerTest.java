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
public class DepartmentControllerTest extends BaseIntegrationTest {
    private static Vertx vertx;
    private static WebClient webClient;
    private static int serverPort;
    private static final String serverHost = "localhost";
    private static final String serverUrl = "/api/v1/departments";

    private final JsonObject department = new JsonObject()
            .put("name", "Computer Science");

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
                stmt.executeUpdate("DELETE FROM departments");
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
    void createDepartmentShouldReturnCreatedDepartment(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(department, testContext.succeeding(response -> testContext.verify(() -> {
                    Assertions.assertEquals(201, response.statusCode());
                    JsonObject createdDepartment = response.bodyAsJsonObject();
                    Assertions.assertNotNull(createdDepartment.getLong("id"));
                    Assertions.assertEquals("Computer Science", createdDepartment.getString("name"));
                    testContext.completeNow();
                })));
    }

    @Test
    void findAllDepartmentsShouldReturnListOfDepartments(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(department, testContext.succeeding(createResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createResponse.statusCode());

                    webClient.get(serverPort, serverHost, serverUrl)
                            .send(testContext.succeeding(findAllResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(200, findAllResponse.statusCode());

                                JsonArray departments = findAllResponse.bodyAsJsonArray();
                                Assertions.assertEquals(1, departments.size());

                                JsonObject returnedDepartment = departments.getJsonObject(0);
                                Assertions.assertNotNull(returnedDepartment.getLong("id"));
                                Assertions.assertEquals("Computer Science", returnedDepartment.getString("name"));
                                testContext.completeNow();
                            })));
                })));
    }

    @Test
    void findAllDepartmentsShouldReturnEmptyList(VertxTestContext testContext) {
        webClient.get(serverPort, serverHost, serverUrl)
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    Assertions.assertEquals(200, response.statusCode());

                    JsonArray departments = response.bodyAsJsonArray();
                    Assertions.assertTrue(departments.isEmpty());

                    testContext.completeNow();
                })));
    }

    @Test
    void findDepartmentByIdShouldReturnDepartment(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(department, testContext.succeeding(createResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createResponse.statusCode());
                    JsonObject createdDepartment = createResponse.bodyAsJsonObject();
                    Long id = createdDepartment.getLong("id");

                    webClient.get(serverPort, serverHost, serverUrl + "/" + id)
                            .send(testContext.succeeding(returnedResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(200, returnedResponse.statusCode());

                                JsonObject returnedDepartment = returnedResponse.bodyAsJsonObject();
                                Assertions.assertEquals(id, returnedDepartment.getLong("id"));
                                Assertions.assertEquals("Computer Science", returnedDepartment.getString("name"));
                                testContext.completeNow();
                            })));
                })));
    }

    @Test
    void findDepartmentByIdShouldThrowException(VertxTestContext testContext) {
        long wrongId = 999L;

        webClient.get(serverPort, serverHost, serverUrl + "/" + wrongId)
                .send(testContext.succeeding(errorResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(404, errorResponse.statusCode());

                    JsonObject errorBody = errorResponse.bodyAsJsonObject();
                    Assertions.assertNotNull(errorBody.getString("timestamp"));
                    Assertions.assertEquals(404, errorBody.getInteger("status"));
                    Assertions.assertEquals("EntityNotFoundException", errorBody.getString("error"));
                    Assertions.assertEquals("Department with ID " + wrongId + " not found", errorBody.getString("message"));
                    testContext.completeNow();
                })));
    }

    @Test
    void updateDepartmentShouldReturnUpdatedDepartment(VertxTestContext testContext) {
        JsonObject updatedDepartment = new JsonObject().put("name", "Updated Department");

        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(department, testContext.succeeding(createResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createResponse.statusCode());
                    JsonObject createdDepartment = createResponse.bodyAsJsonObject();
                    Long id = createdDepartment.getLong("id");

                    webClient.patch(serverPort, serverHost, serverUrl + "/" + id)
                            .sendJson(updatedDepartment, testContext.succeeding(updateResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(200, updateResponse.statusCode());

                                JsonObject returnedDepartment = updateResponse.bodyAsJsonObject();
                                Assertions.assertEquals(id, returnedDepartment.getLong("id"));
                                Assertions.assertEquals("Updated Department", returnedDepartment.getString("name"));

                                testContext.completeNow();
                            })));
                })));
    }

    @Test
    void updateDepartmentShouldThrowException(VertxTestContext testContext) {
        long wrongId = 999L;
        JsonObject updatedDepartment = new JsonObject().put("name", "Updated Department");

        webClient.patch(serverPort, serverHost, serverUrl + "/" + wrongId)
                .sendJson(updatedDepartment, testContext.succeeding(response -> testContext.verify(() -> {
                    Assertions.assertEquals(404, response.statusCode());

                    JsonObject errorBody = response.bodyAsJsonObject();
                    Assertions.assertNotNull(errorBody.getString("timestamp"));
                    Assertions.assertEquals(404, errorBody.getInteger("status"));
                    Assertions.assertEquals("EntityNotFoundException", errorBody.getString("error"));
                    Assertions.assertEquals("Department with ID " + wrongId + " not found", errorBody.getString("message"));

                    testContext.completeNow();
                })));
    }

    @Test
    void deleteDepartmentShouldRemoveEntityFromDatabase(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(department, testContext.succeeding(createResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createResponse.statusCode());
                    JsonObject createdDepartment = createResponse.bodyAsJsonObject();
                    Long id = createdDepartment.getLong("id");

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
    void deleteDepartmentShouldReturnNoContentIfDepartmentNotExists(VertxTestContext testContext) {
        long wrongDepartmentId = 999L;

        webClient.delete(serverPort, serverHost, serverUrl + "/" + wrongDepartmentId)
                .send(testContext.succeeding(deleteResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(204, deleteResponse.statusCode());
                    testContext.completeNow();
                })));
    }

    @Test
    void setTeacherToDepartmentShouldAssociateTeacher(VertxTestContext testContext) {
        JsonObject teacher = new JsonObject().put("name", "John Doe");

        webClient.post(serverPort, serverHost, "/api/v1/teachers")
                .sendJson(teacher, testContext.succeeding(teacherResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, teacherResponse.statusCode());
                    JsonObject createdTeacher = teacherResponse.bodyAsJsonObject();
                    Long teacherId = createdTeacher.getLong("id");

                    webClient.post(serverPort, serverHost, serverUrl)
                            .sendJson(department, testContext.succeeding(departmentResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(201, departmentResponse.statusCode());
                                JsonObject createdDepartment = departmentResponse.bodyAsJsonObject();
                                Long departmentId = createdDepartment.getLong("id");

                                webClient.post(serverPort, serverHost, serverUrl + "/" + departmentId + "/teacher/" + teacherId)
                                        .send(testContext.succeeding(setTeacherResponse -> testContext.verify(() -> {
                                            Assertions.assertEquals(200, setTeacherResponse.statusCode());

                                            JsonObject updatedDepartment = setTeacherResponse.bodyAsJsonObject();
                                            Assertions.assertEquals(departmentId, updatedDepartment.getLong("id"));
                                            Assertions.assertEquals("Computer Science", updatedDepartment.getString("name"));

                                            JsonObject departmentTeacher = updatedDepartment.getJsonObject("headOfDepartment");
                                            Assertions.assertEquals(teacherId, departmentTeacher.getLong("id"));
                                            Assertions.assertEquals("John Doe", departmentTeacher.getString("name"));

                                            testContext.completeNow();
                                        })));
                            })));
                })));
    }

    @Test
    void setTeacherToDepartmentShouldThrowExceptionIfNotFound(VertxTestContext testContext) {
        long wrongDepartmentId = 999L;
        long wrongTeacherId = 888L;

        webClient.post(serverPort, serverHost, serverUrl + "/" + wrongDepartmentId + "/teacher/" + wrongTeacherId)
                .send(testContext.succeeding(errorResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(404, errorResponse.statusCode());

                    JsonObject errorBody = errorResponse.bodyAsJsonObject();
                    Assertions.assertNotNull(errorBody.getString("timestamp"));
                    Assertions.assertEquals(404, errorBody.getInteger("status"));
                    Assertions.assertEquals("EntityNotFoundException", errorBody.getString("error"));
                    Assertions.assertTrue(errorBody.getString("message").contains("not found"));

                    testContext.completeNow();
                })));
    }

}
