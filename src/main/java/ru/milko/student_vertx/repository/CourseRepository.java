package ru.milko.student_vertx.repository;

import io.vertx.core.Future;
import ru.milko.student_vertx.model.Course;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CourseRepository {
    Future<Course> save(Course course);
    Future<List<Course>> findAll();
    Future<List<Course>> findAllByTeacherId(Long teacherId);
    Future<List<Course>> findAllByListOfTeacherIds(List<Long> teacherIds);
    Future<Optional<Course>> findById(Long id);
    Future<Course> update(Course course);
    Future<Void> deleteById(Long id);
    Future<List<Course>> findAllByStudentId(Long id);
    Future<Map<Long, List<Course>>> findAllByListOfStudentIds(List<Long> studentIds);
    Future<Boolean> existsById(Long id);
    Future<Void> setTeacherToCourse(Long courseId, Long teacherId);
}
