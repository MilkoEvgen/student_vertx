package ru.milko.student_vertx.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.milko.student_vertx.dto.DepartmentDto;
import ru.milko.student_vertx.model.Department;

@Mapper
public interface DepartmentMapper {
//    @Mapping(target = "id", ignore = true)
//    void updateFromDto(DepartmentDto dto, @MappingTarget Department department);

    Department toDepartment(DepartmentDto dto);

    DepartmentDto toDepartmentDto(Department department);
}

