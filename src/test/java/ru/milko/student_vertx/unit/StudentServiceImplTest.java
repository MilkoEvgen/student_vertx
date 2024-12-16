package ru.milko.student_vertx.unit;

import io.vertx.core.Future;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.milko.student_vertx.dto.CourseDto;
import ru.milko.student_vertx.dto.StudentDto;
import ru.milko.student_vertx.dto.TeacherDto;
import ru.milko.student_vertx.exceptions.EntityNotFoundException;
import ru.milko.student_vertx.mapper.CourseMapper;
import ru.milko.student_vertx.mapper.StudentMapper;
import ru.milko.student_vertx.mapper.TeacherMapper;
import ru.milko.student_vertx.model.Course;
import ru.milko.student_vertx.model.Student;
import ru.milko.student_vertx.model.Teacher;
import ru.milko.student_vertx.repository.CourseRepository;
import ru.milko.student_vertx.repository.StudentRepository;
import ru.milko.student_vertx.repository.TeacherRepository;
import ru.milko.student_vertx.service.impl.StudentServiceImpl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class StudentServiceImplTest {
    private StudentRepository studentRepository;
    private CourseRepository courseRepository;
    private TeacherRepository teacherRepository;
    private StudentMapper studentMapper;
    private CourseMapper courseMapper;
    private TeacherMapper teacherMapper;
    private StudentServiceImpl studentService;

    private Student student;
    private StudentDto studentDto;
    private Course course;
    private CourseDto courseDto;
    private Teacher teacher;
    private TeacherDto teacherDto;

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        courseRepository = mock(CourseRepository.class);
        teacherRepository = mock(TeacherRepository.class);
        studentMapper = mock(StudentMapper.class);
        courseMapper = mock(CourseMapper.class);
        teacherMapper = mock(TeacherMapper.class);

        studentService = new StudentServiceImpl(
                studentRepository,
                courseRepository,
                teacherRepository,
                studentMapper,
                courseMapper,
                teacherMapper
        );

        student = Student.builder()
                .id(1L)
                .name("John Doe")
                .email("john.doe@mail.com")
                .build();

        studentDto = StudentDto.builder()
                .id(1L)
                .name("John Doe")
                .email("john.doe@mail.com")
                .build();

        course = Course.builder()
                .id(1L)
                .title("Math")
                .teacherId(2L)
                .build();

        courseDto = CourseDto.builder()
                .id(1L)
                .title("Math")
                .build();

        teacher = Teacher.builder()
                .id(2L)
                .name("Dr. Smith")
                .build();

        teacherDto = TeacherDto.builder()
                .id(2L)
                .name("Dr. Smith")
                .build();
    }

    @Test
    void createStudentShouldReturnStudentDto() {
        when(studentMapper.toStudent(any())).thenReturn(student);
        when(studentMapper.toStudentDto(any())).thenReturn(studentDto);
        when(studentRepository.save(any())).thenReturn(Future.succeededFuture(student));

        Future<StudentDto> result = studentService.create(studentDto);

        assertTrue(result.succeeded());
        assertEquals(studentDto, result.result());

        verify(studentMapper).toStudent(any());
        verify(studentRepository).save(any());
        verify(studentMapper).toStudentDto(any());
    }

    @Test
    void createStudentShouldFail() {
        when(studentMapper.toStudent(any())).thenReturn(student);
        when(studentRepository.save(any())).thenReturn(Future.failedFuture("Database error"));

        Future<StudentDto> result = studentService.create(studentDto);

        assertTrue(result.failed());
        assertEquals("Database error", result.cause().getMessage());

        verify(studentMapper).toStudent(any());
        verify(studentRepository).save(any());
        verify(studentMapper, never()).toStudentDto(any());
    }

    @Test
    void findAllShouldReturnListOfStudents() {
        when(studentRepository.findAll()).thenReturn(Future.succeededFuture(List.of(student)));
        when(courseRepository.findAllByListOfStudentIds(any())).thenReturn(Future.succeededFuture(Map.of(1L, List.of(course))));
        when(teacherRepository.findAllByIds(any())).thenReturn(Future.succeededFuture(List.of(teacher)));
        when(studentMapper.toStudentDto(any())).thenReturn(studentDto);
        when(courseMapper.toCourseDto(any())).thenReturn(courseDto);
        when(teacherMapper.toTeacherDto(any())).thenReturn(teacherDto);

        Future<List<StudentDto>> result = studentService.findAll();

        assertTrue(result.succeeded());
        assertEquals(List.of(studentDto), result.result());

        verify(studentRepository).findAll();
        verify(courseRepository).findAllByListOfStudentIds(any());
        verify(teacherRepository).findAllByIds(any());
        verify(studentMapper).toStudentDto(any());
        verify(courseMapper).toCourseDto(any());
        verify(teacherMapper).toTeacherDto(any());
    }

    @Test
    void findAllShouldReturnEmptyList() {
        when(studentRepository.findAll()).thenReturn(Future.succeededFuture(Collections.emptyList()));

        Future<List<StudentDto>> result = studentService.findAll();

        assertTrue(result.succeeded());
        assertEquals(Collections.emptyList(), result.result());

        verify(studentRepository).findAll();
        verify(courseRepository, never()).findAllByListOfStudentIds(any());
        verify(teacherRepository, never()).findAllByIds(any());
        verify(studentMapper, never()).toStudentDto(any());
        verify(courseMapper, never()).toCourseDto(any());
        verify(teacherMapper, never()).toTeacherDto(any());
    }

    @Test
    void findByIdShouldReturnStudentDto() {
        when(studentRepository.findById(any())).thenReturn(Future.succeededFuture(Optional.of(student)));
        when(courseRepository.findAllByStudentId(any())).thenReturn(Future.succeededFuture(List.of(course)));
        when(teacherRepository.findAllByIds(any())).thenReturn(Future.succeededFuture(List.of(teacher)));
        when(studentMapper.toStudentDto(any())).thenReturn(studentDto);
        when(courseMapper.toCourseDto(any())).thenReturn(courseDto);
        when(teacherMapper.toTeacherDto(any())).thenReturn(teacherDto);

        Future<StudentDto> result = studentService.findById(1L);

        assertTrue(result.succeeded());
        assertEquals(studentDto, result.result());

        verify(studentRepository).findById(any());
        verify(courseRepository).findAllByStudentId(any());
        verify(teacherRepository).findAllByIds(any());
        verify(studentMapper).toStudentDto(any());
        verify(courseMapper).toCourseDto(any());
        verify(teacherMapper).toTeacherDto(any());
    }

    @Test
    void findByIdShouldThrowEntityNotFoundException() {
        when(courseRepository.findAllByStudentId(any())).thenReturn(Future.succeededFuture(Collections.emptyList()));
        when(studentRepository.findById(any())).thenReturn(Future.succeededFuture(Optional.empty()));
        when(teacherRepository.findAllByIds(any())).thenReturn(Future.succeededFuture(Collections.emptyList()));

        Future<StudentDto> result = studentService.findById(999L);

        assertTrue(result.failed());
        assertInstanceOf(EntityNotFoundException.class, result.cause());
        assertEquals("Student with ID 999 not found", result.cause().getMessage());

        verify(studentRepository).findById(any());
        verify(courseRepository).findAllByStudentId(any());
        verify(teacherRepository).findAllByIds(any());
        verify(studentMapper, never()).toStudentDto(any());
        verify(courseMapper, never()).toCourseDto(any());
        verify(teacherMapper, never()).toTeacherDto(any());
    }

    @Test
    void findAllCoursesByStudentIdShouldReturnCourses() {
        when(studentRepository.findAllCoursesByStudentId(eq(1L)))
                .thenReturn(Future.succeededFuture(List.of(course)));
        when(courseMapper.toCourseDto(any())).thenReturn(courseDto);
        when(teacherRepository.findAllByIds(any()))
                .thenReturn(Future.succeededFuture(List.of(teacher)));
        when(teacherMapper.toTeacherDto(any())).thenReturn(teacherDto);

        Future<List<CourseDto>> result = studentService.findAllCoursesByStudentId(1L);

        assertTrue(result.succeeded());
        assertEquals(1, result.result().size());
        assertEquals(courseDto, result.result().get(0));
        verify(studentRepository).findAllCoursesByStudentId(eq(1L));
        verify(teacherRepository).findAllByIds(any());
        verify(courseMapper).toCourseDto(any());
    }

    @Test
    void findAllCoursesByStudentIdShouldReturnEmptyList() {
        when(studentRepository.findAllCoursesByStudentId(eq(1L)))
                .thenReturn(Future.succeededFuture(Collections.emptyList()));

        Future<List<CourseDto>> result = studentService.findAllCoursesByStudentId(1L);

        assertTrue(result.succeeded());
        assertTrue(result.result().isEmpty());
        verify(studentRepository).findAllCoursesByStudentId(eq(1L));
        verify(teacherRepository, never()).findAllByIds(any());
        verify(courseMapper, never()).toCourseDto(any());
    }

    @Test
    void updateShouldReturnUpdatedStudentDto() {
        when(studentMapper.toStudent(any())).thenReturn(student);
        when(studentRepository.update(any())).thenReturn(Future.succeededFuture(student));
        when(studentMapper.toStudentDto(any())).thenReturn(studentDto);

        Future<StudentDto> result = studentService.update(studentDto);

        assertTrue(result.succeeded());
        assertEquals(studentDto, result.result());

        verify(studentMapper).toStudent(any());
        verify(studentRepository).update(any());
        verify(studentMapper).toStudentDto(any());
    }

    @Test
    void updateShouldThrowEntityNotFoundException() {
        when(studentMapper.toStudent(any())).thenReturn(student);
        when(studentRepository.update(any()))
                .thenReturn(Future.failedFuture(new EntityNotFoundException("Student with ID 1 not found")));

        Future<StudentDto> result = studentService.update(studentDto);

        assertTrue(result.failed());
        assertInstanceOf(EntityNotFoundException.class, result.cause());
        assertEquals("Student with ID 1 not found", result.cause().getMessage());

        verify(studentMapper).toStudent(any());
        verify(studentRepository).update(any());
        verify(studentMapper, never()).toStudentDto(any());
    }

    @Test
    void deleteByIdShouldSucceed() {
        when(studentRepository.deleteById(eq(1L))).thenReturn(Future.succeededFuture());

        Future<Void> result = studentService.deleteById(1L);

        assertTrue(result.succeeded());
        verify(studentRepository).deleteById(any());
    }

    @Test
    void addCourseToStudentShouldSucceed() {
        when(studentRepository.existsById(any())).thenReturn(Future.succeededFuture(true));
        when(courseRepository.existsById(any())).thenReturn(Future.succeededFuture(true));
        when(studentRepository.addCourseToStudent(any(), any())).thenReturn(Future.succeededFuture());
        when(studentRepository.findById(any())).thenReturn(Future.succeededFuture(Optional.of(student)));
        when(courseRepository.findAllByStudentId(any())).thenReturn(Future.succeededFuture(List.of(course)));
        when(teacherRepository.findAllByIds(any())).thenReturn(Future.succeededFuture(List.of(teacher)));
        when(studentMapper.toStudentDto(any())).thenReturn(studentDto);
        when(courseMapper.toCourseDto(any())).thenReturn(courseDto);
        when(teacherMapper.toTeacherDto(any())).thenReturn(teacherDto);

        Future<StudentDto> result = studentService.addCourseToStudent(1L, 1L);

        assertTrue(result.succeeded());
        assertEquals(studentDto, result.result());

        verify(studentRepository).existsById(any());
        verify(courseRepository).existsById(any());
        verify(studentRepository).addCourseToStudent(any(), any());
        verify(studentRepository).findById(any());
        verify(courseRepository).findAllByStudentId(any());
        verify(teacherRepository).findAllByIds(any());
        verify(studentMapper).toStudentDto(any());
        verify(courseMapper).toCourseDto(any());
        verify(teacherMapper).toTeacherDto(any());

    }

    @Test
    void addCourseToStudentShouldThrowExceptionWhenCourseNotFound() {
        when(studentRepository.existsById(eq(1L))).thenReturn(Future.succeededFuture(true));
        when(courseRepository.existsById(eq(999L))).thenReturn(Future.succeededFuture(false));

        Future<StudentDto> result = studentService.addCourseToStudent(1L, 999L);

        assertTrue(result.failed());
        assertInstanceOf(EntityNotFoundException.class, result.cause());
        assertEquals("Course with ID 999 not found", result.cause().getMessage());

        verify(studentRepository).existsById(eq(1L));
        verify(courseRepository).existsById(eq(999L));
        verify(studentRepository, never()).addCourseToStudent(any(), any());
    }

}
