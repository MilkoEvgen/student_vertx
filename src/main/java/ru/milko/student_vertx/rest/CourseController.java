package ru.milko.student_vertx.rest;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import ru.milko.student_vertx.dto.CourseDto;
import ru.milko.student_vertx.service.CourseService;

import static ru.milko.student_vertx.utils.PathUtils.COURSES_PATH;

@Slf4j
public class CourseController extends BasicController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    public void registerRoutes(Router router) {
        router.post(COURSES_PATH)
                .handler(BodyHandler.create())
                .handler(this::create);

        router.get(COURSES_PATH)
                .handler(this::findAll);

        router.get(COURSES_PATH + "/:id")
                .handler(this::findById);

        router.patch(COURSES_PATH + "/:id")
                .handler(BodyHandler.create())
                .handler(this::update);

        router.delete(COURSES_PATH + "/:id")
                .handler(this::delete);

        router.post(COURSES_PATH + "/:course_id/teacher/:teacher_id")
                .handler(this::setTeacherToCourse);
    }

    private void create(RoutingContext context) {
        CourseDto courseDto = parseRequestBody(context, CourseDto.class);

        courseService.create(courseDto)
                .onSuccess(createdCourse -> respondSuccess(context, 201, createdCourse))
                .onFailure(context::fail);
    }

    private void findAll(RoutingContext context) {
        courseService.findAll()
                .onSuccess(courses -> respondSuccess(context, 200, courses))
                .onFailure(context::fail);
    }

    private void findById(RoutingContext context) {
        Long id = Long.valueOf(context.pathParam("id"));
        courseService.findById(id)
                .onSuccess(course -> {
                    if (course != null) {
                        respondSuccess(context, 200, course);
                    } else {
                        respondError(context, 404, "Course not found");
                    }
                })
                .onFailure(context::fail);
    }

    private void update(RoutingContext context) {
        Long id = Long.valueOf(context.pathParam("id"));
        CourseDto courseDto = parseRequestBody(context, CourseDto.class);
        courseDto.setId(id);

        courseService.update(courseDto)
                .onSuccess(updatedCourse -> respondSuccess(context, 200, updatedCourse))
                .onFailure(context::fail);
    }

    private void delete(RoutingContext context) {
        Long id = Long.valueOf(context.pathParam("id"));
        courseService.deleteById(id)
                .onSuccess(v -> context.response().setStatusCode(204).end())
                .onFailure(context::fail);
    }

    private void setTeacherToCourse(RoutingContext context) {
        Long courseId = Long.valueOf(context.pathParam("course_id"));
        Long teacherId = Long.valueOf(context.pathParam("teacher_id"));
        courseService.setTeacherToCourse(courseId, teacherId)
                .onSuccess(updatedCourse -> respondSuccess(context, 200, updatedCourse))
                .onFailure(context::fail);
    }
}
