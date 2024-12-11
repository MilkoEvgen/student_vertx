package ru.milko.student_vertx.service;

import io.vertx.core.Future;
import ru.milko.student_vertx.dto.DepartmentDto;

import java.util.List;

public interface DepartmentService {
    Future<DepartmentDto> create(DepartmentDto dto);
    Future<List<DepartmentDto>> findAll();
    Future<DepartmentDto> findById(Long id);
    Future<DepartmentDto> update(DepartmentDto dto);
    Future<Void> deleteById(Long id);
    Future<DepartmentDto> setTeacherToDepartment(Long departmentId, Long teacherId);
}
