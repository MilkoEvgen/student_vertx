package ru.milko.student_vertx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department {
    private Long id;
    private String name;
    private Long headOfDepartmentId;
}
