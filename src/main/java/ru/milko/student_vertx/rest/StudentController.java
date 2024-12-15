package ru.milko.student_vertx.rest;

import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import ru.milko.student_vertx.dto.StudentDto;
import ru.milko.student_vertx.service.StudentService;

import static ru.milko.student_vertx.utils.PathUtils.STUDENTS_PATH;

@Slf4j
public class StudentController extends BasicController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    public void registerRoutes(Router router) {
        router.post(STUDENTS_PATH)
                .handler(BodyHandler.create())
                .handler(this::create);

        router.get(STUDENTS_PATH)
                .handler(this::findAll);

        router.get(STUDENTS_PATH + "/:id")
                .handler(this::findById);

        router.get(STUDENTS_PATH + "/:id/courses")
                .handler(this::findAllCoursesByStudentId);

        router.patch(STUDENTS_PATH + "/:id")
                .handler(BodyHandler.create())
                .handler(this::update);

        router.delete(STUDENTS_PATH + "/:id")
                .handler(this::delete);

        router.post(STUDENTS_PATH + "/:studentId/courses/:courseId")
                .handler(this::addCourseToStudent);
    }

    private void create(RoutingContext context) {
        StudentDto studentDto = parseRequestBody(context, StudentDto.class);

        studentService.create(studentDto)
                .onSuccess(createdStudent -> respondSuccess(context, 201, createdStudent))
                .onFailure(context::fail);
    }

    private void findAll(RoutingContext context) {
        studentService.findAll()
                .onSuccess(students -> respondSuccess(context, 200, students))
                .onFailure(context::fail);
    }

    private void findById(RoutingContext context) {
        Long id = Long.valueOf(context.pathParam("id"));
        studentService.findById(id)
                .onSuccess(student -> {
                    if (student != null) {
                        respondSuccess(context, 200, student);
                    } else {
                        respondError(context, 404, "Student not found");
                    }
                })
                .onFailure(context::fail);
    }

    private void findAllCoursesByStudentId(RoutingContext context) {
        Long id = Long.valueOf(context.pathParam("id"));
        studentService.findAllCoursesByStudentId(id)
                .onSuccess(courses -> respondSuccess(context, 200, courses))
                .onFailure(context::fail);
    }

    private void update(RoutingContext context) {
        Long id = Long.valueOf(context.pathParam("id"));
        StudentDto studentDto = parseRequestBody(context, StudentDto.class);
        studentDto.setId(id);

        studentService.update(studentDto)
                .onSuccess(updatedStudent -> respondSuccess(context, 200, updatedStudent))
                .onFailure(context::fail);
    }

    private void delete(RoutingContext context) {
        Long id = Long.valueOf(context.pathParam("id"));
        studentService.deleteById(id)
                .onSuccess(v -> context.response().setStatusCode(204).end())
                .onFailure(context::fail);
    }

    private void addCourseToStudent(RoutingContext context) {
        Long studentId = Long.valueOf(context.pathParam("studentId"));
        Long courseId = Long.valueOf(context.pathParam("courseId"));

        studentService.addCourseToStudent(studentId, courseId)
                .onSuccess(updatedStudent -> respondSuccess(context, 200, updatedStudent))
                .onFailure(context::fail);
    }

}
