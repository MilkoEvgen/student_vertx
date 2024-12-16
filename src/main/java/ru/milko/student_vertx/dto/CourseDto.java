package ru.milko.student_vertx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CourseDto {
    private Long id;
    private String title;
    private TeacherDto teacher;
    private List<StudentDto> students;
}
