package ru.milko.student_vertx.repository;

import io.vertx.core.Future;
import ru.milko.student_vertx.model.Course;
import ru.milko.student_vertx.model.Student;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StudentRepository {
    Future<Student> save(Student student);
    Future<List<Student>> findAll();
    Future<List<Student>> findAllByCourseId(Long courseId);
    Future<Map<Long, List<Student>>> findAllStudentsByCourseIds(List<Long> courseIds);
    Future<Optional<Student>> findById(Long id);
    Future<Student> update(Student student);
    Future<Void> deleteById(Long id);
    Future<List<Course>> findAllCoursesByStudentId(Long id);
    Future<Void> addCourseToStudent(Long studentId, Long courseId);
    Future<Boolean> existsById(Long id);
}
