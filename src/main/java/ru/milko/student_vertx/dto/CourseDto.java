package ru.milko.student_vertx.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class CourseDto {
    private Long id;
    private String title;
    private TeacherDto teacher;
    private List<StudentDto> students;
}
