package ru.milko.student_vertx.service.impl;

import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;
import ru.milko.student_vertx.dto.DepartmentDto;
import ru.milko.student_vertx.dto.TeacherDto;
import ru.milko.student_vertx.exceptions.EntityNotFoundException;
import ru.milko.student_vertx.mapper.DepartmentMapper;
import ru.milko.student_vertx.mapper.TeacherMapper;
import ru.milko.student_vertx.model.Department;
import ru.milko.student_vertx.model.Student;
import ru.milko.student_vertx.model.Teacher;
import ru.milko.student_vertx.repository.DepartmentRepository;
import ru.milko.student_vertx.repository.TeacherRepository;
import ru.milko.student_vertx.service.DepartmentService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final TeacherRepository teacherRepository;
    private final DepartmentMapper departmentMapper;
    private final TeacherMapper teacherMapper;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository, TeacherRepository teacherRepository,
                                 DepartmentMapper departmentMapper, TeacherMapper teacherMapper) {
        this.departmentRepository = departmentRepository;
        this.teacherRepository = teacherRepository;
        this.departmentMapper = departmentMapper;
        this.teacherMapper = teacherMapper;
    }

    @Override
    public Future<DepartmentDto> create(DepartmentDto dto) {
        log.info("*** in create, DepartmentDto = {}", dto);
        Department department = departmentMapper.toDepartment(dto);
        return departmentRepository.save(department)
                .map(departmentMapper::toDepartmentDto);
    }

    @Override
    public Future<List<DepartmentDto>> findAll() {
        log.info("*** in findAll");

        return departmentRepository.findAll()
                .compose(departments -> {
                    List<Long> headOfDepartmentIds = departments.stream()
                            .map(Department::getHeadOfDepartmentId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList();

                    return teacherRepository.findAllByIds(headOfDepartmentIds)
                            .compose(teachers -> {
                                Map<Long, Teacher> teacherMap = teachers.stream()
                                        .collect(Collectors.toMap(Teacher::getId, teacher -> teacher));

                                List<DepartmentDto> departmentDtos = departments.stream()
                                        .map(department -> {
                                            DepartmentDto departmentDto = departmentMapper.toDepartmentDto(department);

                                            if (department.getHeadOfDepartmentId() != null) {
                                                Teacher teacher = teacherMap.get(department.getHeadOfDepartmentId());
                                                if (teacher != null) {
                                                    TeacherDto teacherDto = teacherMapper.toTeacherDto(teacher);
                                                    departmentDto.setHeadOfDepartment(teacherDto);
                                                }
                                            }
                                            return departmentDto;
                                        })
                                        .toList();

                                return Future.succeededFuture(departmentDtos);
                            });
                });
    }


    @Override
    public Future<DepartmentDto> findById(Long id) {
        log.info("*** in findById, id = {}", id);

        return departmentRepository.findById(id)
                .compose(optionalDepartment -> {
                    if (optionalDepartment.isEmpty()) {
                        return Future.failedFuture(new EntityNotFoundException("Department with ID " + id + " not found"));
                    }

                    Department department = optionalDepartment.get();
                    DepartmentDto departmentDto = departmentMapper.toDepartmentDto(department);

                    Long headOfDepartmentId = department.getHeadOfDepartmentId();
                    if (headOfDepartmentId != null) {
                        return teacherRepository.findById(headOfDepartmentId)
                                .compose(optionalTeacher -> {
                                    if (optionalTeacher.isPresent()) {
                                        TeacherDto teacherDto = teacherMapper.toTeacherDto(optionalTeacher.get());
                                        departmentDto.setHeadOfDepartment(teacherDto);
                                    } else {
                                        log.warn("Teacher with ID {} not found", headOfDepartmentId);
                                    }
                                    return Future.succeededFuture(departmentDto);
                                });
                    } else {
                        return Future.succeededFuture(departmentDto);
                    }
                });
    }


    @Override
    public Future<DepartmentDto> update(DepartmentDto dto) {
        log.info("*** in update, DepartmentDto = {}", dto);
        Department department = departmentMapper.toDepartment(dto);
        return departmentRepository.update(department)
                .map(departmentMapper::toDepartmentDto);
    }

    @Override
    public Future<Void> deleteById(Long id) {
        log.info("*** in deleteById, id = {}", id);
        return departmentRepository.deleteById(id);
    }

    @Override
    public Future<DepartmentDto> setTeacherToDepartment(Long departmentId, Long teacherId) {
        log.info("*** in setTeacherToDepartment, departmentId = {}, teacherId = {}", departmentId, teacherId);
        Future<Boolean> departmentExists = departmentRepository.existsById(departmentId);
        Future<Boolean> teacherExists = teacherRepository.existsById(teacherId);

        return Future.all(departmentExists, teacherExists)
                .compose(ignored -> {
                    if (!departmentExists.result()) {
                        return Future.failedFuture(new EntityNotFoundException("Department with ID " + departmentId + " not found"));
                    }
                    if (!teacherExists.result()) {
                        return Future.failedFuture(new EntityNotFoundException("Teacher with ID " + teacherId + " not found"));
                    }
                    return departmentRepository.setTeacherToDepartment(departmentId, teacherId)
                            .flatMap(v -> findById(departmentId));
                });
    }
}
