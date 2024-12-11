package ru.milko.student_vertx.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.milko.student_vertx.dto.TeacherDto;
import ru.milko.student_vertx.model.Teacher;

@Mapper
public interface TeacherMapper {
//    @Mapping(target = "id", ignore = true)
//    void updateFromDto(TeacherDto dto, @MappingTarget Teacher teacher);

    Teacher toTeacher(TeacherDto dto);

    TeacherDto toTeacherDto(Teacher teacher);
}

