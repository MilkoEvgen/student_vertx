package ru.milko.student_vertx.repository;

import io.vertx.core.Future;
import ru.milko.student_vertx.model.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository {
    Future<Department> save(Department department);
    Future<List<Department>> findAll();
    Future<List<Department>> findAllByHeadIds(List<Long> teacherIds);
    Future<Optional<Department>> findById(Long id);
    Future<Optional<Department>> findByHeadOfDepartmentId(Long headOfDepartmentId);
    Future<Department> update(Department department);
    Future<Void> deleteById(Long id);
    Future<Boolean> existsById(Long id);
    Future<Void> setTeacherToDepartment(Long departmentId, Long teacherId);
}
