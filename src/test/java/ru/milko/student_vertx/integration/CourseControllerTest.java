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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.milko.student_vertx.MyVerticle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@ExtendWith(VertxExtension.class)
public class CourseControllerTest extends BaseIntegrationTest {
    private static Vertx vertx;
    private static WebClient webClient;
    private static int serverPort;
    private static final String serverHost = "localhost";
    private static final String serverUrl = "/api/v1/courses";

    private final JsonObject course = new JsonObject().put("title", "Test Course");


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
                stmt.executeUpdate("DELETE FROM courses");
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
    void createCourseTestShouldReturnCreatedCourse(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(course, testContext.succeeding(response -> testContext.verify(() -> {
                    Assertions.assertEquals(201, response.statusCode());
                    JsonObject createdCourse = response.bodyAsJsonObject();
                    Assertions.assertNotNull(createdCourse.getLong("id"));
                    Assertions.assertEquals("Test Course", createdCourse.getString("title"));
                    testContext.completeNow();
                })));
    }

    @Test
    void createDuplicateCourseTestShouldThrowException(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(course, testContext.succeeding(firstResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, firstResponse.statusCode());

                    webClient.post(serverPort, serverHost, serverUrl)
                            .sendJson(course, testContext.succeeding(secondResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(400, secondResponse.statusCode());

                                JsonObject errorBody = secondResponse.bodyAsJsonObject();
                                Assertions.assertNotNull(errorBody.getString("timestamp"));
                                Assertions.assertEquals(400, errorBody.getInteger("status"));
                                Assertions.assertEquals("PgException", errorBody.getString("error"));
                                Assertions.assertEquals("ERROR: duplicate key value violates unique constraint \"courses_title_key\" (23505)", errorBody.getString("message"));
                                Assertions.assertEquals("/api/v1/courses", errorBody.getString("path"));

                                testContext.completeNow();
                            })));
                })));
    }

    @Test
    void findAllShouldReturnListOfCourses(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(course, testContext.succeeding(createResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createResponse.statusCode());

                    webClient.get(serverPort, serverHost, serverUrl)
                            .send(testContext.succeeding(findAllResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(200, findAllResponse.statusCode());

                                JsonArray courses = findAllResponse.bodyAsJsonArray();
                                Assertions.assertEquals(1, courses.size());

                                JsonObject returnedCourse = courses.getJsonObject(0);
                                Assertions.assertNotNull(returnedCourse.getLong("id"));
                                Assertions.assertEquals("Test Course", returnedCourse.getString("title"));

                                testContext.completeNow();
                            })));
                })));
    }

    @Test
    void findAllShouldReturnEmptyList(VertxTestContext testContext) {
        webClient.get(serverPort, serverHost, serverUrl)
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    Assertions.assertEquals(200, response.statusCode());

                    JsonArray courses = response.bodyAsJsonArray();
                    Assertions.assertTrue(courses.isEmpty());

                    testContext.completeNow();
                })));
    }

    @Test
    void findByIdShouldReturnCourse(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(course, testContext.succeeding(createResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createResponse.statusCode());
                    JsonObject createdCourse = createResponse.bodyAsJsonObject();
                    Long id = createdCourse.getLong("id");

                    webClient.get(serverPort, serverHost, serverUrl + "/" + id)
                            .send(testContext.succeeding(returnedResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(200, returnedResponse.statusCode());

                                JsonObject returnedCourse = returnedResponse.bodyAsJsonObject();
                                Assertions.assertEquals(id, returnedCourse.getLong("id"));
                                Assertions.assertEquals("Test Course", returnedCourse.getString("title"));

                                testContext.completeNow();
                            })));
                })));
    }

    @Test
    void findByIdShouldThrowException(VertxTestContext testContext) {
        long wrongId = 999L;

        webClient.get(serverPort, serverHost, serverUrl + "/" + wrongId)
                .send(testContext.succeeding(errorResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(404, errorResponse.statusCode());

                    JsonObject errorBody = errorResponse.bodyAsJsonObject();
                    Assertions.assertNotNull(errorBody.getString("timestamp"));
                    Assertions.assertEquals(404, errorBody.getInteger("status"));
                    Assertions.assertEquals("EntityNotFoundException", errorBody.getString("error"));
                    Assertions.assertEquals("Course with ID " + wrongId + " not found", errorBody.getString("message"));
                    Assertions.assertEquals("/api/v1/courses" + "/" + wrongId, errorBody.getString("path"));

                    testContext.completeNow();
                })));
    }

    @Test
    void updateCourseShouldReturnUpdatedStudent(VertxTestContext testContext) {
        JsonObject updatedCourse = new JsonObject().put("title", "Updated Title");

        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(course, testContext.succeeding(createResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createResponse.statusCode());
                    JsonObject createdCourse = createResponse.bodyAsJsonObject();
                    Long id = createdCourse.getLong("id");

                    webClient.patch(serverPort, serverHost, serverUrl + "/" + id)
                            .sendJson(updatedCourse, testContext.succeeding(updateResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(200, updateResponse.statusCode());

                                JsonObject returnedCourse = updateResponse.bodyAsJsonObject();
                                Assertions.assertEquals(id, returnedCourse.getLong("id"));
                                Assertions.assertEquals("Updated Title", returnedCourse.getString("title"));

                                testContext.completeNow();
                            })));
                })));
    }

    @Test
    void updateStudentShouldThrowException(VertxTestContext testContext) {
        long wrongId = 999L;
        JsonObject updatedCourse = new JsonObject().put("title", "Updated Title");

        webClient.patch(serverPort, serverHost, serverUrl + "/" + wrongId)
                .sendJson(updatedCourse, testContext.succeeding(response -> testContext.verify(() -> {
                    Assertions.assertEquals(404, response.statusCode());

                    JsonObject errorBody = response.bodyAsJsonObject();
                    Assertions.assertNotNull(errorBody.getString("timestamp"));
                    Assertions.assertEquals(404, errorBody.getInteger("status"));
                    Assertions.assertEquals("EntityNotFoundException", errorBody.getString("error"));
                    Assertions.assertEquals("Course with ID " + wrongId + " not found", errorBody.getString("message"));
                    Assertions.assertEquals("/api/v1/courses/" + wrongId, errorBody.getString("path"));

                    testContext.completeNow();
                })));
    }

    @Test
    void deleteCourseShouldRemoveEntityFromDatabase(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(course, testContext.succeeding(createResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createResponse.statusCode());

                    JsonObject createdCourse = createResponse.bodyAsJsonObject();
                    Long courseId = createdCourse.getLong("id");

                    webClient.delete(serverPort, serverHost, serverUrl + "/" + courseId)
                            .send(testContext.succeeding(deleteResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(204, deleteResponse.statusCode());

                                webClient.get(serverPort, serverHost, serverUrl + "/" + courseId)
                                        .send(testContext.succeeding(getResponse -> testContext.verify(() -> {
                                            Assertions.assertEquals(404, getResponse.statusCode());

                                            JsonObject errorBody = getResponse.bodyAsJsonObject();
                                            Assertions.assertEquals(404, errorBody.getInteger("status"));

                                            testContext.completeNow();
                                        })));
                            })));
                })));
    }

    @Test
    void deleteCourseShouldReturnNoContentIfCourseNotExists(VertxTestContext testContext) {
        long wrongCourseId = 999L;

        webClient.delete(serverPort, serverHost, serverUrl + "/" + wrongCourseId)
                .send(testContext.succeeding(deleteResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(204, deleteResponse.statusCode());

                    testContext.completeNow();
                })));
    }

    @Test
    void setTeacherToCourseShouldAssociateTeacherWithCourse(VertxTestContext testContext) {
        JsonObject teacher = new JsonObject().put("name", "John Doe");

        webClient.post(serverPort, serverHost, "/api/v1/teachers")
                .sendJson(teacher, testContext.succeeding(teacherResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, teacherResponse.statusCode());
                    JsonObject createdTeacher = teacherResponse.bodyAsJsonObject();
                    Long teacherId = createdTeacher.getLong("id");
                    System.out.println("*** teacherId = " + teacherId);

                    webClient.post(serverPort, serverHost, serverUrl)
                            .sendJson(course, testContext.succeeding(courseResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(201, courseResponse.statusCode());
                                JsonObject createdCourse = courseResponse.bodyAsJsonObject();
                                Long courseId = createdCourse.getLong("id");
                                System.out.println("*** courseId = " + courseId);

                                webClient.post(serverPort, serverHost, serverUrl + "/" + courseId + "/teacher/" + teacherId)
                                        .send(testContext.succeeding(setTeacherResponse -> testContext.verify(() -> {
                                            Assertions.assertEquals(200, setTeacherResponse.statusCode());

                                            JsonObject updatedCourse = setTeacherResponse.bodyAsJsonObject();
                                            Assertions.assertEquals(courseId, updatedCourse.getLong("id"));
                                            Assertions.assertEquals("Test Course", updatedCourse.getString("title"));
                                            Assertions.assertNotNull(updatedCourse.getJsonObject("teacher"));

                                            JsonObject returnedTeacher = updatedCourse.getJsonObject("teacher");
                                            Assertions.assertEquals(teacherId, returnedTeacher.getLong("id"));
                                            Assertions.assertEquals("John Doe", returnedTeacher.getString("name"));

                                            testContext.completeNow();
                                        })));
                            })));
                })));
    }

    @Test
    void setTeacherToCourseShouldThrowException(VertxTestContext testContext) {
        long wrongCourseId = 999L;
        long wrongTeacherId = 999L;

        webClient.post(serverPort, serverHost, serverUrl + "/" + wrongCourseId + "/teacher/" + wrongTeacherId)
                .send(testContext.succeeding(errorResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(404, errorResponse.statusCode());

                    JsonObject errorBody = errorResponse.bodyAsJsonObject();
                    Assertions.assertNotNull(errorBody.getString("timestamp"));
                    Assertions.assertEquals(404, errorBody.getInteger("status"));
                    Assertions.assertEquals("EntityNotFoundException", errorBody.getString("error"));
                    Assertions.assertEquals("Course with ID " + wrongCourseId + " not found", errorBody.getString("message"));
                    Assertions.assertEquals("/api/v1/courses" + "/" + wrongCourseId + "/teacher/" + wrongTeacherId, errorBody.getString("path"));

                    testContext.completeNow();
                })));
    }

}


