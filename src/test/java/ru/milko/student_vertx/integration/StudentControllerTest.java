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
public class StudentControllerTest extends BaseIntegrationTest {
    private static Vertx vertx;
    private static WebClient webClient;
    private static int serverPort;
    private static final String serverHost = "localhost";
    private static final String serverUrl = "/api/v1/students";

    private final JsonObject student = new JsonObject()
            .put("name", "Test Student")
            .put("email", "test@mail.com");


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
                stmt.executeUpdate("DELETE FROM course_student");
                stmt.executeUpdate("DELETE FROM courses");
                stmt.executeUpdate("DELETE FROM students");
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
    void createStudentTestShouldReturnCreatedStudent(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(student, testContext.succeeding(response -> testContext.verify(() -> {
                    Assertions.assertEquals(201, response.statusCode());
                    JsonObject createdCourse = response.bodyAsJsonObject();
                    Assertions.assertNotNull(createdCourse.getLong("id"));
                    Assertions.assertEquals("Test Student", createdCourse.getString("name"));
                    Assertions.assertEquals("test@mail.com", createdCourse.getString("email"));
                    testContext.completeNow();
                })));
    }

    @Test
    void createDuplicateStudentTestShouldThrowException(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(student, testContext.succeeding(firstResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, firstResponse.statusCode());

                    webClient.post(serverPort, serverHost, serverUrl)
                            .sendJson(student, testContext.succeeding(secondResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(400, secondResponse.statusCode());

                                JsonObject errorBody = secondResponse.bodyAsJsonObject();
                                Assertions.assertNotNull(errorBody.getString("timestamp"));
                                Assertions.assertEquals(400, errorBody.getInteger("status"));
                                Assertions.assertEquals("PgException", errorBody.getString("error"));
                                Assertions.assertEquals("ERROR: duplicate key value violates unique constraint \"students_name_key\" (23505)", errorBody.getString("message"));
                                Assertions.assertEquals("/api/v1/students", errorBody.getString("path"));

                                testContext.completeNow();
                            })));
                })));
    }

    @Test
    void findAllStudentsShouldReturnListOfStudents(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(student, testContext.succeeding(createResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createResponse.statusCode());

                    webClient.get(serverPort, serverHost, serverUrl)
                            .send(testContext.succeeding(findAllResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(200, findAllResponse.statusCode());

                                JsonArray students = findAllResponse.bodyAsJsonArray();
                                Assertions.assertEquals(1, students.size());

                                JsonObject returnedStudent = students.getJsonObject(0);
                                Assertions.assertNotNull(returnedStudent.getLong("id"));
                                Assertions.assertEquals("Test Student", returnedStudent.getString("name"));
                                Assertions.assertEquals("test@mail.com", returnedStudent.getString("email"));

                                testContext.completeNow();
                            })));
                })));
    }

    @Test
    void findAllStudentsShouldReturnEmptyList(VertxTestContext testContext) {
        webClient.get(serverPort, serverHost, serverUrl)
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    Assertions.assertEquals(200, response.statusCode());

                    JsonArray students = response.bodyAsJsonArray();
                    Assertions.assertTrue(students.isEmpty());

                    testContext.completeNow();
                })));
    }

    @Test
    void findStudentByIdShouldReturnStudent(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(student, testContext.succeeding(createResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createResponse.statusCode());
                    JsonObject createdStudent = createResponse.bodyAsJsonObject();
                    Long id = createdStudent.getLong("id");

                    webClient.get(serverPort, serverHost, serverUrl + "/" + id)
                            .send(testContext.succeeding(returnedResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(200, returnedResponse.statusCode());

                                JsonObject returnedStudent = returnedResponse.bodyAsJsonObject();
                                Assertions.assertEquals(id, returnedStudent.getLong("id"));
                                Assertions.assertEquals("Test Student", returnedStudent.getString("name"));
                                Assertions.assertEquals("test@mail.com", returnedStudent.getString("email"));

                                testContext.completeNow();
                            })));
                })));
    }

    @Test
    void findStudentByIdShouldThrowException(VertxTestContext testContext) {
        long wrongId = 999L;

        webClient.get(serverPort, serverHost, serverUrl + "/" + wrongId)
                .send(testContext.succeeding(errorResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(404, errorResponse.statusCode());

                    JsonObject errorBody = errorResponse.bodyAsJsonObject();
                    Assertions.assertNotNull(errorBody.getString("timestamp"));
                    Assertions.assertEquals(404, errorBody.getInteger("status"));
                    Assertions.assertEquals("EntityNotFoundException", errorBody.getString("error"));
                    Assertions.assertEquals("Student with ID " + wrongId + " not found", errorBody.getString("message"));
                    Assertions.assertEquals("/api/v1/students" + "/" + wrongId, errorBody.getString("path"));

                    testContext.completeNow();
                })));
    }

    @Test
    void findAllCoursesByStudentIdShouldReturnCourses(VertxTestContext testContext) {
        JsonObject course = new JsonObject().put("title", "Math");

        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(student, testContext.succeeding(createStudentResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createStudentResponse.statusCode());
                    JsonObject createdStudent = createStudentResponse.bodyAsJsonObject();
                    Long studentId = createdStudent.getLong("id");

                    webClient.post(serverPort, serverHost, "/api/v1/courses")
                            .sendJson(course, testContext.succeeding(createCourseResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(201, createCourseResponse.statusCode());
                                JsonObject createdCourse = createCourseResponse.bodyAsJsonObject();
                                Long courseId = createdCourse.getLong("id");

                                webClient.post(serverPort, serverHost, "/api/v1/students/" + studentId + "/courses/" + courseId)
                                        .send(testContext.succeeding(associateResponse -> testContext.verify(() -> {
                                            Assertions.assertEquals(200, associateResponse.statusCode());

                                            webClient.get(serverPort, serverHost, serverUrl + "/" + studentId + "/courses")
                                                    .send(testContext.succeeding(findCoursesResponse -> testContext.verify(() -> {
                                                        Assertions.assertEquals(200, findCoursesResponse.statusCode());

                                                        JsonArray courses = findCoursesResponse.bodyAsJsonArray();
                                                        Assertions.assertEquals(1, courses.size());
                                                        Assertions.assertEquals("Math", courses.getJsonObject(0).getString("title"));

                                                        testContext.completeNow();
                                                    })));
                                        })));
                            })));
                })));
    }

    @Test
    void findAllCoursesByStudentIdShouldReturnEmptyList(VertxTestContext testContext) {
        long studentId = 999L;

        webClient.get(serverPort, serverHost, serverUrl + "/" + studentId + "/courses")
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    Assertions.assertEquals(200, response.statusCode());

                    JsonArray courses = response.bodyAsJsonArray();
                    Assertions.assertTrue(courses.isEmpty());

                    testContext.completeNow();
                })));
    }

    @Test
    void updateStudentShouldReturnUpdatedStudent(VertxTestContext testContext) {
        JsonObject updatedStudent = new JsonObject()
                .put("name", "Updated Student")
                .put("email", "updated@mail.com");

        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(student, testContext.succeeding(createResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createResponse.statusCode());
                    JsonObject createdStudent = createResponse.bodyAsJsonObject();
                    Long id = createdStudent.getLong("id");

                    webClient.patch(serverPort, serverHost, serverUrl + "/" + id)
                            .sendJson(updatedStudent, testContext.succeeding(updateResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(200, updateResponse.statusCode());

                                JsonObject returnedStudent = updateResponse.bodyAsJsonObject();
                                Assertions.assertEquals(id, returnedStudent.getLong("id"));
                                Assertions.assertEquals("Updated Student", returnedStudent.getString("name"));
                                Assertions.assertEquals("updated@mail.com", returnedStudent.getString("email"));

                                testContext.completeNow();
                            })));
                })));
    }

    @Test
    void updateStudentShouldThrowException(VertxTestContext testContext) {
        long wrongId = 999L;
        JsonObject updatedStudent = new JsonObject()
                .put("name", "Updated Student")
                .put("email", "updated@mail.com");

        webClient.patch(serverPort, serverHost, serverUrl + "/" + wrongId)
                .sendJson(updatedStudent, testContext.succeeding(response -> testContext.verify(() -> {
                    Assertions.assertEquals(404, response.statusCode());

                    JsonObject errorBody = response.bodyAsJsonObject();
                    Assertions.assertNotNull(errorBody.getString("timestamp"));
                    Assertions.assertEquals(404, errorBody.getInteger("status"));
                    Assertions.assertEquals("EntityNotFoundException", errorBody.getString("error"));
                    Assertions.assertEquals("Student with ID " + wrongId + " not found", errorBody.getString("message"));
                    Assertions.assertEquals("/api/v1/students/" + wrongId, errorBody.getString("path"));

                    testContext.completeNow();
                })));
    }

    @Test
    void deleteStudentShouldRemoveEntityFromDatabase(VertxTestContext testContext) {
        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(student, testContext.succeeding(createResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createResponse.statusCode());
                    JsonObject createdStudent = createResponse.bodyAsJsonObject();
                    Long id = createdStudent.getLong("id");

                    webClient.delete(serverPort, serverHost, serverUrl + "/" + id)
                            .send(testContext.succeeding(deleteResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(204, deleteResponse.statusCode());

                                webClient.get(serverPort, serverHost, serverUrl + "/" + id)
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
    void deleteStudentShouldReturnNoContentIfStudentNotExists(VertxTestContext testContext) {
        long wrongStudentId = 999L;

        webClient.delete(serverPort, serverHost, serverUrl + "/" + wrongStudentId)
                .send(testContext.succeeding(deleteResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(204, deleteResponse.statusCode());

                    testContext.completeNow();
                })));
    }

    @Test
    void addCourseToStudentShouldAssociateCourseWithStudent(VertxTestContext testContext) {
        JsonObject course = new JsonObject().put("title", "Math");

        webClient.post(serverPort, serverHost, serverUrl)
                .sendJson(student, testContext.succeeding(createStudentResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(201, createStudentResponse.statusCode());
                    JsonObject createdStudent = createStudentResponse.bodyAsJsonObject();
                    Long studentId = createdStudent.getLong("id");

                    webClient.post(serverPort, serverHost, "/api/v1/courses")
                            .sendJson(course, testContext.succeeding(createCourseResponse -> testContext.verify(() -> {
                                Assertions.assertEquals(201, createCourseResponse.statusCode());
                                JsonObject createdCourse = createCourseResponse.bodyAsJsonObject();
                                Long courseId = createdCourse.getLong("id");

                                webClient.post(serverPort, serverHost, serverUrl + "/" + studentId + "/courses/" + courseId)
                                        .send(testContext.succeeding(associateResponse -> testContext.verify(() -> {
                                            Assertions.assertEquals(200, associateResponse.statusCode());

                                            JsonObject updatedStudent = associateResponse.bodyAsJsonObject();
                                            Assertions.assertEquals(studentId, updatedStudent.getLong("id"));
                                            Assertions.assertEquals("Test Student", updatedStudent.getString("name"));
                                            Assertions.assertEquals("test@mail.com", updatedStudent.getString("email"));
                                            Assertions.assertNotNull(updatedStudent.getJsonArray("courses"));

                                            JsonArray courses = updatedStudent.getJsonArray("courses");
                                            Assertions.assertEquals(1, courses.size());
                                            Assertions.assertEquals("Math", courses.getJsonObject(0).getString("title"));

                                            testContext.completeNow();
                                        })));
                            })));
                })));
    }

    @Test
    void addCourseToStudentShouldThrowExceptionIfStudentOrCourseNotExists(VertxTestContext testContext) {
        long wrongStudentId = 999L;
        long wrongCourseId = 888L;

        webClient.post(serverPort, serverHost, serverUrl + "/" + wrongStudentId + "/courses/" + wrongCourseId)
                .send(testContext.succeeding(errorResponse -> testContext.verify(() -> {
                    Assertions.assertEquals(404, errorResponse.statusCode());

                    JsonObject errorBody = errorResponse.bodyAsJsonObject();
                    Assertions.assertNotNull(errorBody.getString("timestamp"));
                    Assertions.assertEquals(404, errorBody.getInteger("status"));
                    Assertions.assertEquals("EntityNotFoundException", errorBody.getString("error"));
                    Assertions.assertEquals("Student with ID " + wrongStudentId + " not found", errorBody.getString("message"));
                    Assertions.assertEquals("/api/v1/students/" + wrongStudentId + "/courses/" + wrongCourseId, errorBody.getString("path"));

                    testContext.completeNow();
                })));
    }



}
