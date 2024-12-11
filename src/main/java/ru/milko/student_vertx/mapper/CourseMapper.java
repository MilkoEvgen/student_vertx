package ru.milko.student_vertx.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.milko.student_vertx.dto.CourseDto;
import ru.milko.student_vertx.model.Course;

@Mapper
public interface CourseMapper {

//    @Mapping(target = "id", ignore = true)
//    void updateFromDto(CourseDto dto, @MappingTarget Course course);

    Course toCourse(CourseDto dto);

    CourseDto toCourseDto(Course course);
}

