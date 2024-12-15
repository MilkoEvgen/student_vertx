package ru.milko.student_vertx.rest;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import ru.milko.student_vertx.dto.DepartmentDto;
import ru.milko.student_vertx.service.DepartmentService;

import static ru.milko.student_vertx.utils.PathUtils.DEPARTMENTS_PATH;

@Slf4j
public class DepartmentController extends BasicController {
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    public void registerRoutes(Router router) {
        router.post(DEPARTMENTS_PATH)
                .handler(BodyHandler.create())
                .handler(this::create);

        router.get(DEPARTMENTS_PATH)
                .handler(this::findAll);

        router.get(DEPARTMENTS_PATH + "/:id")
                .handler(this::findById);

        router.patch(DEPARTMENTS_PATH + "/:id")
                .handler(BodyHandler.create())
                .handler(this::update);

        router.delete(DEPARTMENTS_PATH + "/:id")
                .handler(this::delete);

        router.post(DEPARTMENTS_PATH + "/:department_id/teacher/:teacher_id")
                .handler(this::setTeacherToDepartment);
    }

    private void create(RoutingContext context) {
        DepartmentDto departmentDto = parseRequestBody(context, DepartmentDto.class);

        departmentService.create(departmentDto)
                .onSuccess(createdDepartment -> respondSuccess(context, 201, createdDepartment))
                .onFailure(context::fail);
    }

    private void findAll(RoutingContext context) {
        departmentService.findAll()
                .onSuccess(departments -> respondSuccess(context, 200, departments))
                .onFailure(context::fail);
    }

    private void findById(RoutingContext context) {
        Long id = Long.valueOf(context.pathParam("id"));
        departmentService.findById(id)
                .onSuccess(department -> {
                    if (department != null) {
                        respondSuccess(context, 200, department);
                    } else {
                        respondError(context, 404, "Department not found");
                    }
                })
                .onFailure(context::fail);
    }

    private void update(RoutingContext context) {
        Long id = Long.valueOf(context.pathParam("id"));
        DepartmentDto departmentDto = parseRequestBody(context, DepartmentDto.class);
        departmentDto.setId(id);

        departmentService.update(departmentDto)
                .onSuccess(updatedDepartment -> respondSuccess(context, 200, updatedDepartment))
                .onFailure(context::fail);
    }

    private void delete(RoutingContext context) {
        Long id = Long.valueOf(context.pathParam("id"));
        departmentService.deleteById(id)
                .onSuccess(v -> context.response().setStatusCode(204).end())
                .onFailure(context::fail);
    }

    private void setTeacherToDepartment(RoutingContext context){
        Long departmentId = Long.valueOf(context.pathParam("department_id"));
        Long teacherId = Long.valueOf(context.pathParam("teacher_id"));

        departmentService.setTeacherToDepartment(departmentId, teacherId)
                .onSuccess(updatedDepartment -> respondSuccess(context, 200, updatedDepartment))
                .onFailure(context::fail);
    }
}
