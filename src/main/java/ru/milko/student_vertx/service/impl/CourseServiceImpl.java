package ru.milko.student_vertx.service.impl;

import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;
import ru.milko.student_vertx.dto.CourseDto;
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
import ru.milko.student_vertx.service.CourseService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;

    public CourseServiceImpl(CourseRepository courseRepository, TeacherRepository teacherRepository, StudentRepository studentRepository, CourseMapper courseMapper, TeacherMapper teacherMapper, StudentMapper studentMapper) {
        this.courseRepository = courseRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.courseMapper = courseMapper;
        this.teacherMapper = teacherMapper;
        this.studentMapper = studentMapper;
    }

    @Override
    public Future<CourseDto> create(CourseDto dto) {
        log.info("*** in create, CourseDto = {}", dto);
        Course course = courseMapper.toCourse(dto);
        return courseRepository.save(course)
                .map(courseMapper::toCourseDto);
    }

    @Override
    public Future<List<CourseDto>> findAll() {
        log.info("*** in findAll");

        return courseRepository.findAll().compose(courses -> {
            if (courses.isEmpty()) {
                return Future.succeededFuture(Collections.emptyList());
            }

            List<Long> courseIds = courses.stream()
                    .map(Course::getId)
                    .toList();

            List<Long> teacherIds = courses.stream()
                    .map(Course::getTeacherId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            Future<Map<Long, TeacherDto>> teacherMapFuture = teacherIds.isEmpty()
                    ? Future.succeededFuture(Collections.emptyMap())
                    : teacherRepository.findAllByIds(teacherIds)
                    .map(teachers -> teachers.stream()
                            .collect(Collectors.toMap(Teacher::getId, teacherMapper::toTeacherDto)));

            Future<Map<Long, List<Student>>> studentsByCourseIdFuture = studentRepository.findAllStudentsByCourseIds(courseIds);

            return Future.all(teacherMapFuture, studentsByCourseIdFuture).map(cf -> {
                Map<Long, TeacherDto> teacherMap = cf.resultAt(0);
                Map<Long, List<Student>> studentsByCourseId = cf.resultAt(1);

                return courses.stream().map(course -> {
                    CourseDto courseDto = courseMapper.toCourseDto(course);

                    if (course.getTeacherId() != null) {
                        courseDto.setTeacher(teacherMap.get(course.getTeacherId()));
                    }

                    List<Student> relatedStudents = studentsByCourseId
                            .getOrDefault(course.getId(), Collections.emptyList());
                    courseDto.setStudents(studentMapper.toStudentDtoList(relatedStudents));

                    return courseDto;
                }).collect(Collectors.toList());
            });
        });
    }


    @Override
    public Future<CourseDto> findById(Long id) {
        log.info("*** in findById, id = {}", id);

        return courseRepository.findById(id)
                .compose(optionalCourse -> {
                    if (optionalCourse.isPresent()) {
                        Course course = optionalCourse.get();
                        CourseDto courseDto = courseMapper.toCourseDto(course);
                        Long teacherId = course.getTeacherId();

                        Future<Void> teacherFuture;
                        if (teacherId != null) {
                            teacherFuture = teacherRepository.findById(teacherId)
                                    .compose(optionalTeacher -> {
                                        if (optionalTeacher.isPresent()) {
                                            Teacher teacher = optionalTeacher.get();
                                            courseDto.setTeacher(teacherMapper.toTeacherDto(teacher));
                                            return Future.succeededFuture();
                                        } else {
                                            return Future.failedFuture(new EntityNotFoundException("Teacher with ID " + teacherId + " not found"));
                                        }
                                    });
                        } else {
                            teacherFuture = Future.succeededFuture();
                        }

                        return teacherFuture.compose(v -> studentRepository.findAllByCourseId(id)
                                .map(students -> {
                                    courseDto.setStudents(studentMapper.toStudentDtoList(students));
                                    return courseDto;
                                })
                        );

                    } else {
                        return Future.failedFuture(new EntityNotFoundException("Course with ID " + id + " not found"));
                    }
                });
    }




    @Override
    public Future<CourseDto> update(CourseDto dto) {
        log.info("*** in update, CourseDto = {}", dto);
        Course course = courseMapper.toCourse(dto);
        return courseRepository.update(course)
                .map(courseMapper::toCourseDto);
    }

    @Override
    public Future<Void> deleteById(Long id) {
        log.info("*** in deleteById, id = {}", id);
        return courseRepository.deleteById(id);
    }


    @Override
    public Future<CourseDto> setTeacherToCourse(Long courseId, Long teacherId) {
        log.info("*** in setTeacherToCourse, courseId = {}, teacherId = {}", courseId, teacherId);

        Future<Boolean> courseExists = courseRepository.existsById(courseId);
        Future<Boolean> teacherExists = teacherRepository.existsById(teacherId);

        return Future.all(courseExists, teacherExists)
                .compose(ignored -> {
                    if (!courseExists.result()) {
                        return Future.failedFuture(new EntityNotFoundException("Course with ID " + courseId + " not found"));
                    }
                    if (!teacherExists.result()) {
                        return Future.failedFuture(new EntityNotFoundException("Teacher with ID " + teacherId + " not found"));
                    }

                    return courseRepository.setTeacherToCourse(courseId, teacherId)
                            .flatMap(v -> findById(courseId));
                });
    }
}
