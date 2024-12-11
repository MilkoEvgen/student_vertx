package ru.milko.student_vertx.repository;

import io.vertx.core.Future;
import ru.milko.student_vertx.model.Teacher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherRepository {
    Future<Teacher> save(Teacher teacher);
    Future<List<Teacher>> findAll();
    Future<Optional<Teacher>> findById(Long id);
    Future<Teacher> update(Teacher teacher);
    Future<Void> deleteById(Long id);
    Future<Boolean> existsById(Long id);
    Future<List<Teacher>> findAllByIds(List<Long> ids);
}
