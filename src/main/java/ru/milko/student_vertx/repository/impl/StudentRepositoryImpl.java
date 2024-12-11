package ru.milko.student_vertx.repository.impl;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.extern.slf4j.Slf4j;
import ru.milko.student_vertx.model.Course;
import ru.milko.student_vertx.model.Student;
import ru.milko.student_vertx.repository.StudentRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class StudentRepositoryImpl implements StudentRepository {
    private final Pool client;

    public StudentRepositoryImpl(Pool client) {
        this.client = client;
    }

    public Future<Student> save(Student student) {
        log.info("*** in save, student = {}", student);
        String query = "INSERT INTO students (name, email) VALUES ($1, $2) RETURNING id";

        return client.preparedQuery(query).execute(Tuple.of(student.getName(), student.getEmail())).map(rows -> {
            Row row = rows.iterator().next();
            student.setId(row.getLong("id"));
            return student;
        });
    }

    public Future<List<Student>> findAll() {
        log.info("*** in findAll");
        String query = "SELECT id, name, email FROM students";

        return client.query(query).execute().map(rows -> {
            List<Student> students = new ArrayList<>();
            for (Row row : rows) {
                students.add(Student.builder()
                        .id(row.getLong("id"))
                        .name(row.getString("name"))
                        .email(row.getString("email"))
                        .build());
            }
            return students;
        });
    }

    @Override
    public Future<List<Student>> findAllByCourseId(Long courseId) {
        log.info("*** in findAllByCourseId, courseId = {}", courseId);
        String query = "SELECT * FROM students s " +
                "INNER JOIN course_student cs " +
                "ON s.id = cs.student_id " +
                "WHERE cs.course_id = $1";

        return client.preparedQuery(query)
                .execute(Tuple.of(courseId))
                .map(rows -> {
                    List<Student> students = new ArrayList<>();
                    for (Row row : rows) {
                        students.add(Student.builder()
                                .id(row.getLong("id"))
                                .name(row.getString("name"))
                                .email(row.getString("email"))
                                .build());
                    }
                    return students;
                });
    }

    public Future<Map<Long, List<Student>>> findAllStudentsByCourseIds(List<Long> courseIds) {
        String query = "SELECT cs.course_id, s.id, s.name, s.email " +
                "FROM students s " +
                "INNER JOIN course_student cs ON s.id = cs.student_id " +
                "WHERE cs.course_id = ANY($1)";

        Long[] courseIdsArray = courseIds.toArray(new Long[0]);

        return client.preparedQuery(query)
                .execute(Tuple.of((Object) courseIdsArray))
                .map(rows -> {
                    Map<Long, List<Student>> studentsByCourseId = new HashMap<>();
                    for (Row row : rows) {
                        Long courseId = row.getLong("course_id");
                        Student student = Student.builder()
                                .id(row.getLong("id"))
                                .name(row.getString("name"))
                                .email(row.getString("email"))
                                .build();

                        studentsByCourseId
                                .computeIfAbsent(courseId, k -> new ArrayList<>())
                                .add(student);
                    }
                    return studentsByCourseId;
                });
    }


    public Future<Optional<Student>> findById(Long id) {
        log.info("*** in findById, id = {}", id);
        String query = "SELECT id, name, email FROM students WHERE id = $1";

        return client.preparedQuery(query).execute(Tuple.of(id))
                .map(rows -> {
                    if (rows.rowCount() == 0) {
                        return Optional.empty();
                    }
                    Row row = rows.iterator().next();
                    return Optional.of(Student.builder()
                            .id(row.getLong("id"))
                            .name(row.getString("name"))
                            .email(row.getString("email"))
                            .build());
                });
    }


    @Override
    public Future<Student> update(Student student) {
        log.info("*** in update, course = {}", student);
        StringBuilder queryBuilder = new StringBuilder("UPDATE students SET ");
        Tuple params = Tuple.tuple();
        int paramIndex = 1;

        if (student.getName() != null) {
            queryBuilder.append("name = $").append(paramIndex++).append(", ");
            params.addString(student.getName());
        }
        if (student.getEmail() != null) {
            queryBuilder.append("email = $").append(paramIndex++).append(", ");
            params.addString(student.getEmail());
        }

        queryBuilder.setLength(queryBuilder.length() - 2);

        queryBuilder.append(" WHERE id = $").append(paramIndex);
        params.addLong(student.getId());

        String query = queryBuilder.toString();

        return client.preparedQuery(query)
                .execute(params)
                .compose(result -> {
                    if (result.rowCount() > 0) {
                        return Future.succeededFuture(student);
                    } else {
                        return Future.failedFuture("No rows were updated");
                    }
                });
    }

    public Future<Void> deleteById(Long id) {
        log.info("*** in deleteById, id = {}", id);
        String query = "DELETE FROM students WHERE id = $1";
        return client.preparedQuery(query).execute(Tuple.of(id)).mapEmpty();
    }

    @Override
    public Future<List<Course>> findAllCoursesByStudentId(Long id) {
        log.info("*** in getAllCoursesByStudentId, StudentId = {}", id);
        String query = "SELECT c.id, c.title, c.teacher_id FROM courses c " +
                "INNER JOIN course_student cs ON c.id = cs.course_id " +
                "WHERE cs.student_id = $1";

        return client.preparedQuery(query).execute(Tuple.of(id)).map(rows -> {
            List<Course> courses = new ArrayList<>();
            for (Row row : rows) {
                courses.add(Course.builder()
                        .id(row.getLong("id"))
                        .title(row.getString("title"))
                        .teacherId(row.getLong("teacher_id"))
                        .build());
            }
            return courses;
        });
    }

    @Override
    public Future<Void> addCourseToStudent(Long studentId, Long courseId) {
        log.info("*** in addCourseToStudent, studentId = {}, courseId = {}", studentId, courseId);

        String query = "INSERT INTO course_student (student_id, course_id) VALUES ($1, $2)";
        return client.preparedQuery(query)
                .execute(Tuple.of(studentId, courseId))
                .map(rows -> null);
    }

    public Future<Boolean> existsById(Long id) {
        log.info("*** in existsById, id = {}", id);

        String sql = "SELECT EXISTS (SELECT 1 FROM students WHERE id = $1)";
        return client
                .preparedQuery(sql)
                .execute(Tuple.of(id))
                .map(rowSet -> {
                    Row row = rowSet.iterator().next();
                    return row.getBoolean(0);
                });
    }

}
