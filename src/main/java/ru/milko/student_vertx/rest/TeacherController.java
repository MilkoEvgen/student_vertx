package ru.milko.student_vertx.rest;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import ru.milko.student_vertx.dto.TeacherDto;
import ru.milko.student_vertx.service.TeacherService;

import static ru.milko.student_vertx.utils.PathUtils.TEACHERS_PATH;

public class TeacherController extends BasicController{
    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    public void registerRoutes(Router router) {
        router.post(TEACHERS_PATH)
                .handler(BodyHandler.create())
                .handler(this::create);

        router.get(TEACHERS_PATH)
                .handler(this::findAll);

        router.get(TEACHERS_PATH + "/:id")
                .handler(this::findById);

        router.patch(TEACHERS_PATH + "/:id")
                .handler(BodyHandler.create())
                .handler(this::update);

        router.delete(TEACHERS_PATH + "/:id")
                .handler(this::delete);
    }

    private void create(RoutingContext context) {
        TeacherDto teacherDto = parseRequestBody(context, TeacherDto.class);

        teacherService.create(teacherDto)
                .onSuccess(createdTeacher -> respondSuccess(context, 201, createdTeacher))
                .onFailure(err -> respondError(context, 500, "Failed to create teacher: " + err.getMessage()));
    }

    private void findAll(RoutingContext context) {
        teacherService.findAll()
                .onSuccess(teachers -> respondSuccess(context, 200, teachers))
                .onFailure(err -> respondError(context, 500, "Failed to retrieve teachers: " + err.getMessage()));
    }

    private void findById(RoutingContext context) {
        Long id = Long.valueOf(context.pathParam("id"));
        teacherService.findById(id)
                .onSuccess(teacher -> {
                    if (teacher != null) {
                        respondSuccess(context, 200, teacher);
                    } else {
                        respondError(context, 404, "Teacher not found");
                    }
                })
                .onFailure(err -> respondError(context, 500, "Failed to retrieve teacher: " + err.getMessage()));
    }

    private void update(RoutingContext context) {
        Long id = Long.valueOf(context.pathParam("id"));
        TeacherDto teacherDto = parseRequestBody(context, TeacherDto.class);
        teacherDto.setId(id);

        teacherService.update(teacherDto)
                .onSuccess(updatedTeacher -> respondSuccess(context, 200, updatedTeacher))
                .onFailure(err -> respondError(context, 500, "Failed to update teacher: " + err.getMessage()));
    }

    private void delete(RoutingContext context) {
        Long id = Long.valueOf(context.pathParam("id"));
        teacherService.deleteById(id)
                .onSuccess(v -> context.response().setStatusCode(204).end())
                .onFailure(err -> respondError(context, 500, "Failed to delete teacher: " + err.getMessage()));
    }
}
