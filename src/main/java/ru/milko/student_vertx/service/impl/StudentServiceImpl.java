package ru.milko.student_vertx.service.impl;

import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;
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
import ru.milko.student_vertx.service.StudentService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class StudentServiceImpl implements StudentService {
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final StudentMapper studentMapper;
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;

    public StudentServiceImpl(StudentRepository studentRepository, CourseRepository courseRepository, TeacherRepository teacherRepository,
                              StudentMapper studentMapper, CourseMapper courseMapper, TeacherMapper teacherMapper) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.teacherRepository = teacherRepository;
        this.studentMapper = studentMapper;
        this.courseMapper = courseMapper;
        this.teacherMapper = teacherMapper;
    }

    @Override
    public Future<StudentDto> create(StudentDto dto) {
        log.info("*** in create, StudentDto = {}", dto);
        Student student = studentMapper.toStudent(dto);
        return studentRepository.save(student)
                .map(studentMapper::toStudentDto);
    }

    @Override
    public Future<List<StudentDto>> findAll() {
        log.info("*** in findAll");
        Future<List<Student>> studentsFuture = studentRepository.findAll();

        return studentsFuture.compose(students -> {
            if (students.isEmpty()) {
                return Future.succeededFuture(Collections.emptyList());
            }
            List<Long> studentIds = students.stream()
                    .map(Student::getId)
                    .toList();

            Future<Map<Long, List<Course>>> coursesByStudentIdFuture = courseRepository.findAllByListOfStudentIds(studentIds);

            return coursesByStudentIdFuture.compose(coursesByStudentId -> {
                List<Long> teacherIds = coursesByStudentId.values().stream()
                        .flatMap(List::stream)
                        .map(Course::getTeacherId)
                        .distinct()
                        .toList();

                if (teacherIds.isEmpty()) {
                    return Future.succeededFuture(
                            students.stream()
                                    .map(student -> {
                                        StudentDto studentDto = studentMapper.toStudentDto(student);
                                        List<CourseDto> courseDtos = coursesByStudentId
                                                .getOrDefault(student.getId(), Collections.emptyList())
                                                .stream()
                                                .map(courseMapper::toCourseDto)
                                                .toList();
                                        studentDto.setCourses(courseDtos);
                                        return studentDto;
                                    })
                                    .toList()
                    );
                }

                return teacherRepository.findAllByIds(teacherIds).map(teachers -> {
                    Map<Long, TeacherDto> teacherMap = teachers.stream()
                            .collect(Collectors.toMap(Teacher::getId, teacherMapper::toTeacherDto));

                    return students.stream()
                            .map(student -> {
                                StudentDto studentDto = studentMapper.toStudentDto(student);
                                List<CourseDto> courseDtos = coursesByStudentId
                                        .getOrDefault(student.getId(), Collections.emptyList())
                                        .stream()
                                        .map(course -> {
                                            CourseDto courseDto = courseMapper.toCourseDto(course);
                                            courseDto.setTeacher(teacherMap.get(course.getTeacherId()));
                                            return courseDto;
                                        })
                                        .toList();
                                studentDto.setCourses(courseDtos);
                                return studentDto;
                            })
                            .toList();
                });
            });
        });
    }



    @Override
    public Future<StudentDto> findById(Long id) {
        log.info("*** in findById, id = {}", id);
        Future<List<Course>> coursesFuture = courseRepository.findAllByStudentId(id);
        Future<Optional<Student>> studentFuture = studentRepository.findById(id);

        return coursesFuture.compose(courses -> {

            List<Long> teacherIds = courses.stream()
                    .map(Course::getTeacherId)
                    .distinct()
                    .toList();

            return teacherRepository.findAllByIds(teacherIds).map(teachers -> {
                Map<Long, TeacherDto> teacherMap = teachers.stream()
                        .collect(Collectors.toMap(Teacher::getId, teacherMapper::toTeacherDto));

                return courses.stream()
                        .map(course -> {
                            CourseDto dto = courseMapper.toCourseDto(course);
                            dto.setTeacher(teacherMap.get(course.getTeacherId()));
                            return dto;
                        })
                        .toList();
            });
        }).compose(courseDtosWithTeachers -> {
            return studentFuture.map(optionalStudent -> optionalStudent.map(student -> {
                StudentDto studentDto = studentMapper.toStudentDto(student);
                studentDto.setCourses(courseDtosWithTeachers);
                return studentDto;
            }).orElseThrow(() -> new EntityNotFoundException("Student with ID " + id + " not found")));
        });
    }


    @Override
    public Future<List<CourseDto>> findAllCoursesByStudentId(Long id) {
        log.info("*** in findAllCoursesByStudentId, StudentId = {}", id);
        return studentRepository.findAllCoursesByStudentId(id)
                .compose(courses -> {
                    if (courses.isEmpty()) {
                        return Future.succeededFuture(Collections.emptyList());
                    }

                    List<Long> teacherIds = courses.stream()
                            .map(Course::getTeacherId)
                            .distinct()
                            .toList();

                    if (teacherIds.isEmpty()) {
                        List<CourseDto> courseDtos = courses.stream()
                                .map(courseMapper::toCourseDto)
                                .collect(Collectors.toList());
                        return Future.succeededFuture(courseDtos);
                    }

                    return teacherRepository.findAllByIds(teacherIds)
                            .map(teachers -> {
                                Map<Long, TeacherDto> teacherMap = teachers.stream()
                                        .collect(Collectors.toMap(Teacher::getId, teacherMapper::toTeacherDto));

                                return courses.stream()
                                        .map(course -> {
                                            CourseDto courseDto = courseMapper.toCourseDto(course);
                                            courseDto.setTeacher(teacherMap.get(course.getTeacherId()));
                                            return courseDto;
                                        })
                                        .collect(Collectors.toList());
                            });
                });
    }


    @Override
    public Future<StudentDto> update(StudentDto dto) {
        log.info("*** in update, StudentDto = {}", dto);
        Student student = studentMapper.toStudent(dto);
        return studentRepository.update(student)
                .map(studentMapper::toStudentDto);
    }

    @Override
    public Future<Void> deleteById(Long id) {
        log.info("*** in deleteById, id = {}", id);
        return studentRepository.deleteById(id);
    }

    @Override
    public Future<StudentDto> addCourseToStudent(Long studentId, Long courseId) {
        log.info("*** in addCourseToStudent, studentId = {}, courseId = {}", studentId, courseId);

        Future<Boolean> studentExists = studentRepository.existsById(studentId);
        Future<Boolean> courseExists = courseRepository.existsById(courseId);

        return Future.all(studentExists, courseExists)
                .compose(ignored -> {
                    if (!studentExists.result()) {
                        return Future.failedFuture(new EntityNotFoundException("Student with ID " + studentId + " not found"));
                    }
                    if (!courseExists.result()) {
                        return Future.failedFuture(new EntityNotFoundException("Course with ID " + courseId + " not found"));
                    }
                    return studentRepository.addCourseToStudent(studentId, courseId)
                            .flatMap(v -> findById(studentId));
                });
    }

}
