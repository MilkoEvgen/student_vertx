package ru.milko.student_vertx.repository.impl;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.extern.slf4j.Slf4j;
import ru.milko.student_vertx.model.Course;
import ru.milko.student_vertx.repository.CourseRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class CourseRepositoryImpl implements CourseRepository {
    private final Pool client;

    public CourseRepositoryImpl(Pool client) {
        this.client = client;
    }

    @Override
    public Future<Course> save(Course course) {
        log.info("*** in save, course = {}", course);
        String query = "INSERT INTO courses (title) VALUES ($1) RETURNING id";

        return client.preparedQuery(query).execute(Tuple.of(course.getTitle())).map(rows -> {
            Row row = rows.iterator().next();
            course.setId(row.getLong("id"));
            return course;
        });
    }

    @Override
    public Future<List<Course>> findAll() {
        log.info("*** in findAll");
        String query = "SELECT id, title, teacher_id FROM courses";

        return client.query(query).execute().map(rows -> {
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
    public Future<List<Course>> findAllByTeacherId(Long teacherId) {
        log.info("*** in findAllCoursesByTeacherId, teacherId = {}", teacherId);
        String query = "SELECT id, title FROM courses " +
                "WHERE teacher_id = $1";

        return client.preparedQuery(query)
                .execute(Tuple.of(teacherId))
                .map(rows -> {
                    List<Course> courses = new ArrayList<>();
                    for (Row row : rows) {
                        courses.add(Course.builder()
                                .id(row.getLong("id"))
                                .title(row.getString("title"))
                                .build());
                    }
                    return courses;
                });
    }

    @Override
    public Future<List<Course>> findAllByListOfTeacherIds(List<Long> teacherIds) {
        log.info("*** in findAllByListOfTeacherIds, teacherIds = {}", teacherIds);
        String query = "SELECT id, title, teacher_id FROM courses WHERE teacher_id = ANY($1)";

        Long[] teacherIdsArray = teacherIds.toArray(new Long[0]);
        return client.preparedQuery(query)
                .execute(Tuple.of((Object) teacherIdsArray))
                .map(rows -> {
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
    public Future<Optional<Course>> findById(Long id) {
        log.info("*** in findById, id = {}", id);
        String query = "SELECT id, title, teacher_id FROM courses WHERE id = $1";

        return client.preparedQuery(query).execute(Tuple.of(id))
                .map(rows -> {
                    if (rows.rowCount() == 0) {
                        return Optional.empty();
                    }
                    Row row = rows.iterator().next();
                    return Optional.of(Course.builder()
                            .id(row.getLong("id"))
                            .title(row.getString("title"))
                            .teacherId(row.getLong("teacher_id"))
                            .build());
                });
    }

    @Override
    public Future<Course> update(Course course) {
        log.info("*** in update, course = {}", course);
        StringBuilder queryBuilder = new StringBuilder("UPDATE courses SET ");
        Tuple params = Tuple.tuple();
        int paramIndex = 1;

        if (course.getTitle() != null) {
            queryBuilder.append("title = $").append(paramIndex++).append(", ");
            params.addString(course.getTitle());
        }

        queryBuilder.setLength(queryBuilder.length() - 2);

        queryBuilder.append(" WHERE id = $").append(paramIndex);
        params.addLong(course.getId());

        String query = queryBuilder.toString();

        return client.preparedQuery(query)
                .execute(params)
                .compose(result -> {
                    if (result.rowCount() > 0) {
                        return Future.succeededFuture(course);
                    } else {
                        return Future.failedFuture("No rows were updated");
                    }
                });
    }

    @Override
    public Future<Void> deleteById(Long id) {
        log.info("*** in deleteById, id = {}", id);
        String query = "DELETE FROM courses WHERE id = $1";
        return client.preparedQuery(query).execute(Tuple.of(id)).mapEmpty();
    }

    @Override
    public Future<List<Course>> findAllByStudentId(Long id) {
        String query = "SELECT * FROM courses c " +
                "INNER JOIN course_student cs " +
                "ON c.id = cs.course_id " +
                "WHERE cs.student_id = $1";

        return client.preparedQuery(query)
                .execute(Tuple.of(id))
                .map(rows -> {
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
    public Future<Map<Long, List<Course>>> findAllByListOfStudentIds(List<Long> studentIds) {
        String query = "SELECT cs.student_id AS student_id, c.id, c.title, c.teacher_id " +
                "FROM courses c " +
                "INNER JOIN course_student cs ON c.id = cs.course_id " +
                "WHERE cs.student_id = ANY($1)";

        Long[] studentIdsArray = studentIds.toArray(new Long[0]);

        return client.preparedQuery(query)
                .execute(Tuple.of(studentIdsArray))
                .map(rows -> {
                    Map<Long, List<Course>> coursesByStudentId = new HashMap<>();
                    for (Row row : rows) {
                        Long sId = row.getLong("student_id");
                        Course course = Course.builder()
                                .id(row.getLong("id"))
                                .title(row.getString("title"))
                                .teacherId(row.getLong("teacher_id"))
                                .build();

                        coursesByStudentId
                                .computeIfAbsent(sId, k -> new ArrayList<>())
                                .add(course);
                    }
                    return coursesByStudentId;
                });

    }


    public Future<Boolean> existsById(Long id) {
        log.info("*** in existsById, id = {}", id);

        String query = "SELECT EXISTS (SELECT 1 FROM courses WHERE id = $1)";
        return client
                .preparedQuery(query)
                .execute(Tuple.of(id))
                .map(rowSet -> {
                    Row row = rowSet.iterator().next();
                    return row.getBoolean(0);
                });
    }

    @Override
    public Future<Void> setTeacherToCourse(Long courseId, Long teacherId) {
        log.info("*** in setTeacherToCourse, courseId = {}, teacherId = {}", courseId, teacherId);
        String query = "UPDATE courses SET teacher_id = $1 WHERE id = $2";

        return client.preparedQuery(query)
                .execute(Tuple.of(teacherId, courseId))
                .map(rows -> null);
    }

}
