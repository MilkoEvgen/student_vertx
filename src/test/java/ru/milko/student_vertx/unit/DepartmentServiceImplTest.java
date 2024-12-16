package ru.milko.student_vertx.unit;

import io.vertx.core.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.milko.student_vertx.dto.DepartmentDto;
import ru.milko.student_vertx.exceptions.EntityNotFoundException;
import ru.milko.student_vertx.mapper.DepartmentMapper;
import ru.milko.student_vertx.mapper.TeacherMapper;
import ru.milko.student_vertx.model.Department;
import ru.milko.student_vertx.model.Teacher;
import ru.milko.student_vertx.repository.DepartmentRepository;
import ru.milko.student_vertx.repository.TeacherRepository;
import ru.milko.student_vertx.service.impl.DepartmentServiceImpl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DepartmentServiceImplTest {
    private DepartmentRepository departmentRepository;
    private TeacherRepository teacherRepository;
    private DepartmentMapper departmentMapper;
    private TeacherMapper teacherMapper;
    private DepartmentServiceImpl departmentService;

    private Department department;
    private DepartmentDto departmentDto;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        departmentRepository = mock(DepartmentRepository.class);
        teacherRepository = mock(TeacherRepository.class);
        departmentMapper = mock(DepartmentMapper.class);
        teacherMapper = mock(TeacherMapper.class);

        departmentService = new DepartmentServiceImpl(
                departmentRepository,
                teacherRepository,
                departmentMapper,
                teacherMapper
        );

        department = Department.builder()
                .id(1L)
                .name("Computer Science")
                .build();

        departmentDto = DepartmentDto.builder()
                .id(1L)
                .name("Computer Science")
                .build();

        teacher = Teacher.builder()
                .id(2L)
                .name("Dr. Smith")
                .build();
    }

    @Test
    void createShouldReturnDepartmentDto() {
        when(departmentMapper.toDepartment(any())).thenReturn(department);
        when(departmentRepository.save(any())).thenReturn(Future.succeededFuture(department));
        when(departmentMapper.toDepartmentDto(any())).thenReturn(departmentDto);

        Future<DepartmentDto> result = departmentService.create(departmentDto);

        assertTrue(result.succeeded());
        assertEquals(departmentDto, result.result());

        verify(departmentMapper).toDepartment(any());
        verify(departmentRepository).save(any());
        verify(departmentMapper).toDepartmentDto(any());
    }

    @Test
    void createShouldFail() {
        when(departmentMapper.toDepartment(any())).thenReturn(department);
        when(departmentRepository.save(any())).thenReturn(Future.failedFuture("Database error"));

        Future<DepartmentDto> result = departmentService.create(departmentDto);

        assertTrue(result.failed());
        assertEquals("Database error", result.cause().getMessage());

        verify(departmentMapper).toDepartment(any());
        verify(departmentRepository).save(any());
        verify(departmentMapper, never()).toDepartmentDto(any());
    }

    @Test
    void findAllShouldReturnListOfDepartments() {
        when(departmentRepository.findAll()).thenReturn(Future.succeededFuture(List.of(department)));
        when(teacherRepository.findAllByIds(any())).thenReturn(Future.succeededFuture(List.of(teacher)));
        when(departmentMapper.toDepartmentDto(any())).thenReturn(departmentDto);

        Future<List<DepartmentDto>> result = departmentService.findAll();

        assertTrue(result.succeeded());
        assertEquals(1, result.result().size());
        assertEquals(departmentDto, result.result().getFirst());

        verify(departmentRepository).findAll();
        verify(teacherRepository).findAllByIds(any());
        verify(departmentMapper).toDepartmentDto(any());
    }

    @Test
    void findAllShouldReturnEmptyList() {
        when(departmentRepository.findAll()).thenReturn(Future.succeededFuture(Collections.emptyList()));
        when(teacherRepository.findAllByIds(any())).thenReturn(Future.succeededFuture(Collections.emptyList()));

        Future<List<DepartmentDto>> result = departmentService.findAll();

        assertTrue(result.succeeded());
        assertTrue(result.result().isEmpty());

        verify(departmentRepository).findAll();
        verify(teacherRepository).findAllByIds(any());
        verify(departmentMapper, never()).toDepartmentDto(any());
    }

    @Test
    void findByIdShouldReturnDepartmentDto() {
        when(departmentRepository.findById(any())).thenReturn(Future.succeededFuture(Optional.of(department)));
        when(departmentMapper.toDepartmentDto(any())).thenReturn(departmentDto);

        Future<DepartmentDto> result = departmentService.findById(1L);

        assertTrue(result.succeeded());
        assertEquals(departmentDto, result.result());

        verify(departmentRepository).findById(any());
        verify(departmentMapper).toDepartmentDto(any());
    }

    @Test
    void findByIdShouldThrowEntityNotFoundException() {
        when(departmentRepository.findById(any())).thenReturn(Future.succeededFuture(Optional.empty()));

        Future<DepartmentDto> result = departmentService.findById(1L);

        assertTrue(result.failed());
        assertInstanceOf(EntityNotFoundException.class, result.cause());
        assertEquals("Department with ID 1 not found", result.cause().getMessage());

        verify(departmentRepository).findById(any());
        verify(departmentMapper, never()).toDepartmentDto(any());
        verify(teacherRepository, never()).findById(any());
    }

    @Test
    void updateShouldReturnUpdatedDepartmentDto() {
        when(departmentMapper.toDepartment(any())).thenReturn(department);
        when(departmentRepository.update(any())).thenReturn(Future.succeededFuture(department));
        when(departmentMapper.toDepartmentDto(any())).thenReturn(departmentDto);

        Future<DepartmentDto> result = departmentService.update(departmentDto);

        assertTrue(result.succeeded());
        assertEquals(departmentDto, result.result());

        verify(departmentMapper).toDepartment(any());
        verify(departmentRepository).update(any());
        verify(departmentMapper).toDepartmentDto(any());
    }

    @Test
    void updateShouldThrowEntityNotFoundException() {
        when(departmentMapper.toDepartment(any())).thenReturn(department);
        when(departmentRepository.update(any()))
                .thenReturn(Future.failedFuture(new EntityNotFoundException("Department with ID 1 not found")));

        Future<DepartmentDto> result = departmentService.update(departmentDto);

        assertTrue(result.failed());
        assertInstanceOf(EntityNotFoundException.class, result.cause());
        assertEquals("Department with ID 1 not found", result.cause().getMessage());

        verify(departmentMapper).toDepartment(any());
        verify(departmentRepository).update(any());
        verify(departmentMapper, never()).toDepartmentDto(any());
    }

    @Test
    void deleteByIdShouldSucceed() {
        when(departmentRepository.deleteById(any())).thenReturn(Future.succeededFuture());

        Future<Void> result = departmentService.deleteById(1L);

        assertTrue(result.succeeded());
        verify(departmentRepository).deleteById(any());
    }

    @Test
    void deleteByIdShouldFail() {
        when(departmentRepository.deleteById(any())).thenReturn(Future.failedFuture("Deletion failed"));

        Future<Void> result = departmentService.deleteById(1L);

        assertTrue(result.failed());
        assertEquals("Deletion failed", result.cause().getMessage());
        verify(departmentRepository).deleteById(any());
    }

    @Test
    void setTeacherToDepartmentShouldSucceed() {
        when(departmentRepository.existsById(any())).thenReturn(Future.succeededFuture(true));
        when(teacherRepository.existsById(any())).thenReturn(Future.succeededFuture(true));
        when(departmentRepository.setTeacherToDepartment(any(), any())).thenReturn(Future.succeededFuture());
        when(departmentRepository.findById(any())).thenReturn(Future.succeededFuture(Optional.of(department)));
        when(departmentMapper.toDepartmentDto(any())).thenReturn(departmentDto);

        Future<DepartmentDto> result = departmentService.setTeacherToDepartment(1L, 2L);

        assertTrue(result.succeeded());
        assertEquals(departmentDto, result.result());

        verify(departmentRepository).existsById(any());
        verify(teacherRepository).existsById(any());
        verify(departmentRepository).setTeacherToDepartment(any(), any());
        verify(departmentRepository).findById(any());
        verify(departmentMapper).toDepartmentDto(any());
        verify(teacherMapper, never()).toTeacherDto(any());
    }

    @Test
    void setTeacherToDepartmentShouldThrowExceptionWhenTeacherNotFound() {
        when(departmentRepository.existsById(any())).thenReturn(Future.succeededFuture(true));
        when(teacherRepository.existsById(any())).thenReturn(Future.succeededFuture(false));

        Future<DepartmentDto> result = departmentService.setTeacherToDepartment(1L, 999L);

        assertTrue(result.failed());
        assertInstanceOf(EntityNotFoundException.class, result.cause());
        assertEquals("Teacher with ID 999 not found", result.cause().getMessage());

        verify(departmentRepository).existsById(any());
        verify(teacherRepository).existsById(any());
        verify(departmentRepository, never()).setTeacherToDepartment(any(), any());
    }
}

