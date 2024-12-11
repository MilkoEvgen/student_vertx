package ru.milko.student_vertx.service;

import io.vertx.core.Future;
import ru.milko.student_vertx.dto.TeacherDto;

import java.util.List;

public interface TeacherService {
    Future<TeacherDto> create(TeacherDto dto);
    Future<List<TeacherDto>> findAll();
    Future<TeacherDto> findById(Long id);
    Future<TeacherDto> update(TeacherDto dto);
    Future<Void> deleteById(Long id);
}
