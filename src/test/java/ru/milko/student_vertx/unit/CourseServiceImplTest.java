package ru.milko.student_vertx.unit;

import io.vertx.core.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.milko.student_vertx.dto.CourseDto;
import ru.milko.student_vertx.dto.StudentDto;
import ru.milko.student_vertx.exceptions.EntityNotFoundException;
import ru.milko.student_vertx.mapper.CourseMapper;
import ru.milko.student_vertx.mapper.StudentMapper;
import ru.milko.student_vertx.mapper.TeacherMapper;
import ru.milko.student_vertx.model.Course;
import ru.milko.student_vertx.model.Student;
import ru.milko.student_vertx.repository.CourseRepository;
import ru.milko.student_vertx.repository.StudentRepository;
import ru.milko.student_vertx.repository.TeacherRepository;
import ru.milko.student_vertx.service.impl.CourseServiceImpl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CourseServiceImplTest {
    private CourseRepository courseRepository;
    private TeacherRepository teacherRepository;
    private StudentRepository studentRepository;
    private CourseMapper courseMapper;
    private TeacherMapper teacherMapper;
    private StudentMapper studentMapper;
    private CourseServiceImpl courseService;

    private Course course;
    private CourseDto courseDto;
    private Student student;
    private StudentDto studentDto;

    @BeforeEach
    void setUp() {
        courseRepository = mock(CourseRepository.class);
        teacherRepository = mock(TeacherRepository.class);
        studentRepository = mock(StudentRepository.class);
        courseMapper = mock(CourseMapper.class);
        teacherMapper = mock(TeacherMapper.class);
        studentMapper = mock(StudentMapper.class);

        courseService = new CourseServiceImpl(
                courseRepository,
                teacherRepository,
                studentRepository,
                courseMapper,
                teacherMapper,
                studentMapper
        );

        course = Course.builder()
                .id(1L)
                .title("Math")
                .build();

        courseDto = CourseDto.builder()
                .id(1L)
                .title("Math")
                .build();

        student = Student.builder()
                .id(1L)
                .name("Student")
                .email("student@mail.com")
                .build();

        studentDto = StudentDto.builder()
                .id(1L)
                .name("Student")
                .email("student@mail.com")
                .build();
    }

    @Test
    void createCourseShouldReturnCourseDto() {
        when(courseMapper.toCourse(any())).thenReturn(course);
        when(courseMapper.toCourseDto(any())).thenReturn(courseDto);
        when(courseRepository.save(any())).thenReturn(Future.succeededFuture(course));

        Future<CourseDto> result = courseService.create(courseDto);

        assertTrue(result.succeeded());
        assertEquals(courseDto, result.result());

        verify(courseRepository).save(any());
        verify(courseMapper).toCourse(any());
        verify(courseMapper).toCourseDto(any());
    }

    @Test
    void createCourseShouldFail() {
        when(courseRepository.save(any())).thenReturn(Future.failedFuture("Database error"));

        Future<CourseDto> result = courseService.create(courseDto);

        assertTrue(result.failed());
        assertEquals("Database error", result.cause().getMessage());

        verify(courseRepository).save(any());
        verify(courseMapper).toCourse(any());
        verify(courseMapper, never()).toCourseDto(any());
    }

    @Test
    void findAllShouldReturnListOfCourses() {
        when(courseRepository.findAll()).thenReturn(Future.succeededFuture(List.of(course)));
        when(studentRepository.findAllStudentsByCourseIds(any())).thenReturn(Future.succeededFuture(Map.of(student.getId(), List.of(student))));
        when(courseMapper.toCourseDto(any(Course.class))).thenReturn(courseDto);

        Future<List<CourseDto>> result = courseService.findAll();

        assertTrue(result.succeeded());
        assertEquals(List.of(courseDto), result.result());
        verify(courseRepository).findAll();
        verify(studentRepository).findAllStudentsByCourseIds(any());
        verify(courseMapper).toCourseDto(any());
    }

    @Test
    void findAllShouldReturnEmptyList() {
        when(courseRepository.findAll()).thenReturn(Future.succeededFuture(Collections.emptyList()));

        Future<List<CourseDto>> result = courseService.findAll();

        assertTrue(result.succeeded());
        assertEquals(Collections.emptyList(), result.result());
        verify(courseRepository).findAll();
        verify(studentRepository, never()).findAllStudentsByCourseIds(any());
        verify(courseMapper, never()).toCourseDto(any());
    }


    @Test
    void findByIdShouldReturnCourseDto() {
        when(courseRepository.findById(any())).thenReturn(Future.succeededFuture(Optional.of(course)));
        when(studentRepository.findAllByCourseId(any())).thenReturn(Future.succeededFuture(List.of(student)));
        when(courseMapper.toCourseDto(any())).thenReturn(courseDto);
        when(studentMapper.toStudentDtoList(any())).thenReturn(List.of(studentDto));

        Future<CourseDto> result = courseService.findById(1L);

        assertTrue(result.succeeded());
        assertEquals(courseDto, result.result());

        verify(courseRepository).findById(any());
        verify(studentRepository).findAllByCourseId(any());
        verify(courseMapper).toCourseDto(any());
        verify(studentMapper).toStudentDtoList(any());
    }


    @Test
    void findByIdShouldThrowEntityNotFoundException() {
        Long courseId = 999L;
        when(courseRepository.findById(any())).thenReturn(Future.succeededFuture(Optional.empty()));

        Future<CourseDto> result = courseService.findById(courseId);

        assertTrue(result.failed());
        assertInstanceOf(EntityNotFoundException.class, result.cause());
        assertEquals("Course with ID 999 not found", result.cause().getMessage());

        verify(courseRepository).findById(any());
        verify(courseMapper, never()).toCourseDto(any());
    }

    @Test
    void updateShouldReturnUpdatedCourseDto() {
        when(courseMapper.toCourse(any())).thenReturn(course);
        when(courseRepository.update(any())).thenReturn(Future.succeededFuture(course));
        when(courseMapper.toCourseDto(any())).thenReturn(courseDto);

        Future<CourseDto> result = courseService.update(courseDto);

        assertTrue(result.succeeded());
        assertEquals(courseDto, result.result());

        verify(courseMapper).toCourse(any());
        verify(courseRepository).update(any());
        verify(courseMapper).toCourseDto(any());
    }

    @Test
    void updateShouldThrowEntityNotFoundException() {
        when(courseMapper.toCourse(any())).thenReturn(course);
        when(courseRepository.update(any()))
                .thenReturn(Future.failedFuture(new EntityNotFoundException("Course with ID " + course.getId() + " not found")));

        Future<CourseDto> result = courseService.update(courseDto);

        assertTrue(result.failed());
        assertInstanceOf(EntityNotFoundException.class, result.cause());
        assertEquals("Course with ID " + course.getId() + " not found", result.cause().getMessage());

        verify(courseMapper).toCourse(any());
        verify(courseRepository).update(any());
        verify(courseMapper, never()).toCourseDto(any());
    }


    @Test
    void deleteByIdShouldSucceed() {
        Long courseId = 1L;
        when(courseRepository.deleteById(any())).thenReturn(Future.succeededFuture());

        Future<Void> result = courseService.deleteById(courseId);

        assertTrue(result.succeeded());
        verify(courseRepository).deleteById(any());
    }

    @Test
    void deleteByIdShouldFail() {
        Long courseId = 1L;

        when(courseRepository.deleteById(any())).thenReturn(Future.failedFuture("Deletion failed"));

        Future<Void> result = courseService.deleteById(courseId);

        assertTrue(result.failed());
        assertEquals("Deletion failed", result.cause().getMessage());
        verify(courseRepository).deleteById(any());
    }

    @Test
    void setTeacherToCourseShouldSucceed() {
        Long courseId = 1L;
        Long teacherId = 2L;

        when(courseRepository.existsById(any())).thenReturn(Future.succeededFuture(true));
        when(teacherRepository.existsById(any())).thenReturn(Future.succeededFuture(true));
        when(courseRepository.setTeacherToCourse(any(), any())).thenReturn(Future.succeededFuture());
        when(courseRepository.findById(any())).thenReturn(Future.succeededFuture(Optional.of(course)));
        when(studentRepository.findAllByCourseId(any())).thenReturn(Future.succeededFuture(List.of(student)));
        when(courseMapper.toCourseDto(any())).thenReturn(courseDto);
        when(studentMapper.toStudentDtoList(any())).thenReturn(List.of(studentDto));

        Future<CourseDto> result = courseService.setTeacherToCourse(courseId, teacherId);

        assertTrue(result.succeeded());
        verify(courseRepository).existsById(any());
        verify(teacherRepository).existsById(any());
        verify(courseRepository).setTeacherToCourse(any(), any());
        verify(courseRepository).findById(any());
        verify(studentRepository).findAllByCourseId(any());
        verify(courseMapper).toCourseDto(any());
        verify(studentMapper).toStudentDtoList(any());
    }

    @Test
    void setTeacherToCourseShouldThrowExceptionWhenTeacherNotFound() {
        Long courseId = 1L;
        Long teacherId = 999L;

        when(courseRepository.existsById(eq(courseId))).thenReturn(Future.succeededFuture(true));
        when(teacherRepository.existsById(eq(teacherId))).thenReturn(Future.succeededFuture(false));

        Future<CourseDto> result = courseService.setTeacherToCourse(courseId, teacherId);

        assertTrue(result.failed());
        assertInstanceOf(EntityNotFoundException.class, result.cause());
        assertEquals("Teacher with ID 999 not found", result.cause().getMessage());

        verify(courseRepository).existsById(eq(courseId));
        verify(teacherRepository).existsById(eq(teacherId));
        verify(courseRepository, never()).setTeacherToCourse(any(), any());
    }
}

