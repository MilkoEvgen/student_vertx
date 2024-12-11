package ru.milko.student_vertx.service.impl;

import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;
import ru.milko.student_vertx.dto.CourseDto;
import ru.milko.student_vertx.dto.DepartmentDto;
import ru.milko.student_vertx.dto.TeacherDto;
import ru.milko.student_vertx.exceptions.EntityNotFoundException;
import ru.milko.student_vertx.mapper.CourseMapper;
import ru.milko.student_vertx.mapper.DepartmentMapper;
import ru.milko.student_vertx.mapper.TeacherMapper;
import ru.milko.student_vertx.model.Course;
import ru.milko.student_vertx.model.Department;
import ru.milko.student_vertx.model.Teacher;
import ru.milko.student_vertx.repository.CourseRepository;
import ru.milko.student_vertx.repository.DepartmentRepository;
import ru.milko.student_vertx.repository.TeacherRepository;
import ru.milko.student_vertx.service.TeacherService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class TeacherServiceImpl implements TeacherService {
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final TeacherMapper teacherMapper;
    private final CourseMapper courseMapper;
    private final DepartmentMapper departmentMapper;

    public TeacherServiceImpl(TeacherRepository teacherRepository, CourseRepository courseRepository,
                              DepartmentRepository departmentRepository, TeacherMapper teacherMapper,
                              CourseMapper courseMapper, DepartmentMapper departmentMapper) {
        this.teacherRepository = teacherRepository;
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
        this.teacherMapper = teacherMapper;
        this.courseMapper = courseMapper;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public Future<TeacherDto> create(TeacherDto dto) {
        log.info("*** in create, TeacherDto = {}", dto);
        Teacher teacher = teacherMapper.toTeacher(dto);
        return teacherRepository.save(teacher)
                .map(teacherMapper::toTeacherDto);
    }

    @Override
    public Future<List<TeacherDto>> findAll() {
        log.info("*** in findAll");

        return teacherRepository.findAll()
                .compose(teachers -> {
                    if (teachers.isEmpty()) {
                        return Future.succeededFuture(Collections.emptyList());
                    }

                    List<TeacherDto> teacherDtos = teachers.stream()
                            .map(teacherMapper::toTeacherDto)
                            .collect(Collectors.toList());

                    List<Long> teacherIds = teachers.stream()
                            .map(Teacher::getId)
                            .collect(Collectors.toList());

                    Future<Map<Long, List<Course>>> coursesByTeacherIdFuture = courseRepository.findAllByListOfTeacherIds(teacherIds)
                            .map(courses -> courses.stream()
                                    .collect(Collectors.groupingBy(Course::getTeacherId)));

                    Future<Map<Long, Department>> departmentsByHeadIdFuture = departmentRepository.findAllByHeadIds(teacherIds)
                            .map(departments -> departments.stream()
                                    .collect(Collectors.toMap(Department::getHeadOfDepartmentId, department -> department)));

                    return Future.all(coursesByTeacherIdFuture, departmentsByHeadIdFuture)
                            .map(results -> {
                                Map<Long, List<Course>> coursesByTeacherId = results.resultAt(0);
                                Map<Long, Department> departmentsByHeadId = results.resultAt(1);

                                teacherDtos.forEach(teacherDto -> {
                                    Long teacherId = teacherDto.getId();

                                    List<Course> courses = coursesByTeacherId.getOrDefault(teacherId, Collections.emptyList());
                                    List<CourseDto> courseDtos = courses.stream()
                                            .map(courseMapper::toCourseDto)
                                            .collect(Collectors.toList());
                                    teacherDto.setCourses(courseDtos);

                                    Department department = departmentsByHeadId.get(teacherId);
                                    if (department != null) {
                                        teacherDto.setDepartment(departmentMapper.toDepartmentDto(department));
                                    }
                                });

                                return teacherDtos;
                            });
                });
    }


    @Override
    public Future<TeacherDto> findById(Long id) {
        log.info("*** in findById, id = {}", id);

        return teacherRepository.findById(id)
                .compose(optionalTeacher -> {
                    if (optionalTeacher.isEmpty()) {
                        return Future.failedFuture(new EntityNotFoundException("Teacher with ID " + id + " not found"));
                    }

                    Teacher teacher = optionalTeacher.get();
                    TeacherDto teacherDto = teacherMapper.toTeacherDto(teacher);

                    Future<List<Course>> coursesFuture = courseRepository.findAllByTeacherId(id);
                    Future<Optional<Department>> optionalDepartmentFuture = departmentRepository.findByHeadOfDepartmentId(id);

                    return Future.all(coursesFuture, optionalDepartmentFuture)
                            .compose(results -> {
                                List<Course> courses = results.resultAt(0);
                                List<CourseDto> courseDtos = courses.stream()
                                        .map(courseMapper::toCourseDto)
                                        .toList();
                                teacherDto.setCourses(courseDtos);

                                Optional<Department> optionalDepartment = results.resultAt(1);
                                optionalDepartment.ifPresent(department -> {
                                    DepartmentDto departmentDto = departmentMapper.toDepartmentDto(department);
                                    teacherDto.setDepartment(departmentDto);
                                });

                                return Future.succeededFuture(teacherDto);
                            });
                });
    }


    @Override
    public Future<TeacherDto> update(TeacherDto dto) {
        log.info("*** in update, TeacherDto = {}", dto);
        Teacher teacher = teacherMapper.toTeacher(dto);
        return teacherRepository.update(teacher)
                .map(teacherMapper::toTeacherDto);
    }

    @Override
    public Future<Void> deleteById(Long id) {
        log.info("*** in deleteById, id = {}", id);
        return teacherRepository.deleteById(id);
    }
}
