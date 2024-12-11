package ru.milko.student_vertx.service;


import io.vertx.core.Future;
import ru.milko.student_vertx.dto.CourseDto;
import ru.milko.student_vertx.dto.StudentDto;

import java.util.List;

public interface StudentService {
    Future<StudentDto> create(StudentDto dto);
    Future<List<StudentDto>> findAll();
    Future<StudentDto> findById(Long id);
    Future<List<CourseDto>> findAllCoursesByStudentId(Long id);
    Future<StudentDto> update(StudentDto dto);
    Future<Void> deleteById(Long id);
    Future<StudentDto> addCourseToStudent(Long studentId, Long courseId);
}
