package ru.milko.student_vertx.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.milko.student_vertx.dto.StudentDto;
import ru.milko.student_vertx.model.Student;

import java.util.List;

@Mapper
public interface StudentMapper {

//    @Mapping(target = "id", ignore = true)
//    void updateFromDto(StudentDto dto, @MappingTarget Student student);

    Student toStudent(StudentDto dto);

    StudentDto toStudentDto(Student student);

    List<StudentDto> toStudentDtoList(List<Student> students);
}

