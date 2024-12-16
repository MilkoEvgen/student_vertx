package ru.milko.student_vertx.unit;

import io.vertx.core.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import ru.milko.student_vertx.service.impl.TeacherServiceImpl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TeacherServiceImplTest {
    private TeacherRepository teacherRepository;
    private CourseRepository courseRepository;
    private DepartmentRepository departmentRepository;
    private TeacherMapper teacherMapper;
    private CourseMapper courseMapper;
    private DepartmentMapper departmentMapper;

    private TeacherServiceImpl teacherService;

    private Teacher teacher;
    private TeacherDto teacherDto;
    private Course course;
    private CourseDto courseDto;
    private Department department;
    private DepartmentDto departmentDto;

    @BeforeEach
    void setUp() {
        teacherRepository = mock(TeacherRepository.class);
        courseRepository = mock(CourseRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        teacherMapper = mock(TeacherMapper.class);
        courseMapper = mock(CourseMapper.class);
        departmentMapper = mock(DepartmentMapper.class);

        teacherService = new TeacherServiceImpl(
                teacherRepository,
                courseRepository,
                departmentRepository,
                teacherMapper,
                courseMapper,
                departmentMapper
        );

        teacher = Teacher.builder()
                .id(1L)
                .name("Dr. John")
                .build();

        teacherDto = TeacherDto.builder()
                .id(1L)
                .name("Dr. John")
                .build();

        course = Course.builder()
                .id(1L)
                .title("Math")
                .teacherId(teacher.getId())
                .build();

        courseDto = CourseDto.builder()
                .id(1L)
                .title("Math")
                .build();

        department = Department.builder()
                .id(1L)
                .name("Science")
                .build();

        departmentDto = DepartmentDto.builder()
                .id(1L)
                .name("Science")
                .build();
    }

    @Test
    void createShouldReturnTeacherDto() {
        when(teacherMapper.toTeacher(any())).thenReturn(teacher);
        when(teacherRepository.save(any())).thenReturn(Future.succeededFuture(teacher));
        when(teacherMapper.toTeacherDto(any())).thenReturn(teacherDto);

        Future<TeacherDto> result = teacherService.create(teacherDto);

        assertTrue(result.succeeded());
        assertEquals(teacherDto, result.result());

        verify(teacherMapper).toTeacher(any());
        verify(teacherRepository).save(any());
        verify(teacherMapper).toTeacherDto(any());
    }

    @Test
    void createShouldFail() {
        when(teacherMapper.toTeacher(any())).thenReturn(teacher);
        when(teacherRepository.save(any())).thenReturn(Future.failedFuture("Database error"));

        Future<TeacherDto> result = teacherService.create(teacherDto);

        assertTrue(result.failed());
        assertEquals("Database error", result.cause().getMessage());

        verify(teacherMapper).toTeacher(any());
        verify(teacherRepository).save(any());
        verify(teacherMapper, never()).toTeacherDto(any());
    }

    @Test
    void findAllShouldReturnListOfTeachers() {
        when(teacherRepository.findAll()).thenReturn(Future.succeededFuture(List.of(teacher)));
        when(courseRepository.findAllByListOfTeacherIds(any())).thenReturn(Future.succeededFuture(List.of(course)));
        when(departmentRepository.findAllByHeadIds(any())).thenReturn(Future.succeededFuture(List.of(department)));
        when(teacherMapper.toTeacherDto(any())).thenReturn(teacherDto);
        when(courseMapper.toCourseDto(any())).thenReturn(courseDto);

        Future<List<TeacherDto>> result = teacherService.findAll();

        assertTrue(result.succeeded());
        assertEquals(1, result.result().size());
        assertEquals(teacherDto, result.result().get(0));

        verify(teacherRepository).findAll();
        verify(courseRepository).findAllByListOfTeacherIds(any());
        verify(departmentRepository).findAllByHeadIds(any());
        verify(teacherMapper).toTeacherDto(any());
        verify(courseMapper).toCourseDto(any());
        verify(departmentMapper, never()).toDepartmentDto(any());
    }

    @Test
    void findAllShouldReturnEmptyList() {
        when(teacherRepository.findAll()).thenReturn(Future.succeededFuture(Collections.emptyList()));

        Future<List<TeacherDto>> result = teacherService.findAll();

        assertTrue(result.succeeded());
        assertTrue(result.result().isEmpty());

        verify(teacherRepository).findAll();
        verify(courseRepository, never()).findAllByListOfTeacherIds(any());
        verify(departmentRepository, never()).findAllByHeadIds(any());
        verify(teacherMapper, never()).toTeacherDto(any());
    }

    @Test
    void findByIdShouldReturnTeacherDto() {
        when(teacherRepository.findById(any())).thenReturn(Future.succeededFuture(Optional.of(teacher)));
        when(courseRepository.findAllByTeacherId(any())).thenReturn(Future.succeededFuture(List.of(course)));
        when(departmentRepository.findByHeadOfDepartmentId(any())).thenReturn(Future.succeededFuture(Optional.of(department)));
        when(teacherMapper.toTeacherDto(any())).thenReturn(teacherDto);
        when(courseMapper.toCourseDto(any())).thenReturn(courseDto);
        when(departmentMapper.toDepartmentDto(any())).thenReturn(departmentDto);

        Future<TeacherDto> result = teacherService.findById(1L);

        assertTrue(result.succeeded());
        assertEquals(teacherDto, result.result());

        verify(teacherRepository).findById(any());
        verify(courseRepository).findAllByTeacherId(any());
        verify(departmentRepository).findByHeadOfDepartmentId(any());
        verify(teacherMapper).toTeacherDto(any());
        verify(courseMapper).toCourseDto(any());
        verify(departmentMapper).toDepartmentDto(any());
    }

    @Test
    void findByIdShouldThrowEntityNotFoundException() {
        when(teacherRepository.findById(any())).thenReturn(Future.succeededFuture(Optional.empty()));

        Future<TeacherDto> result = teacherService.findById(1L);

        assertTrue(result.failed());
        assertInstanceOf(EntityNotFoundException.class, result.cause());
        assertEquals("Teacher with ID 1 not found", result.cause().getMessage());

        verify(teacherRepository).findById(any());
        verify(courseRepository, never()).findAllByTeacherId(any());
        verify(departmentRepository, never()).findByHeadOfDepartmentId(any());
        verify(teacherMapper, never()).toTeacherDto(any());
    }

    @Test
    void updateShouldReturnUpdatedTeacherDto() {
        when(teacherMapper.toTeacher(any())).thenReturn(teacher);
        when(teacherRepository.update(any())).thenReturn(Future.succeededFuture(teacher));
        when(teacherMapper.toTeacherDto(any())).thenReturn(teacherDto);

        Future<TeacherDto> result = teacherService.update(teacherDto);

        assertTrue(result.succeeded());
        assertEquals(teacherDto, result.result());

        verify(teacherMapper).toTeacher(any());
        verify(teacherRepository).update(any());
        verify(teacherMapper).toTeacherDto(any());
    }

    @Test
    void updateShouldThrowEntityNotFoundException() {
        when(teacherMapper.toTeacher(any())).thenReturn(teacher);
        when(teacherRepository.update(any()))
                .thenReturn(Future.failedFuture(new EntityNotFoundException("Teacher with ID 1 not found")));

        Future<TeacherDto> result = teacherService.update(teacherDto);

        assertTrue(result.failed());
        assertInstanceOf(EntityNotFoundException.class, result.cause());
        assertEquals("Teacher with ID 1 not found", result.cause().getMessage());

        verify(teacherMapper).toTeacher(any());
        verify(teacherRepository).update(any());
        verify(teacherMapper, never()).toTeacherDto(any());
    }

    @Test
    void deleteByIdShouldSucceed() {
        when(teacherRepository.deleteById(any())).thenReturn(Future.succeededFuture());

        Future<Void> result = teacherService.deleteById(1L);

        assertTrue(result.succeeded());
        verify(teacherRepository).deleteById(any());
    }
}

