package ru.milko.student_vertx.service;

import io.vertx.core.Future;
import ru.milko.student_vertx.dto.CourseDto;

import java.util.List;

public interface CourseService {
    Future<CourseDto> create(CourseDto dto);
    Future<List<CourseDto>> findAll();
    Future <CourseDto> findById(Long id);
    Future<CourseDto> update(CourseDto dto);
    Future<Void> deleteById(Long id);
    Future<CourseDto> setTeacherToCourse(Long courseId, Long teacherId);
}
