package ru.milko.student_vertx.repository.impl;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.extern.slf4j.Slf4j;
import ru.milko.student_vertx.model.Teacher;
import ru.milko.student_vertx.repository.TeacherRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class TeacherRepositoryImpl implements TeacherRepository {
    private final Pool client;

    public TeacherRepositoryImpl(Pool client) {
        this.client = client;
    }

    @Override
    public Future<Teacher> save(Teacher teacher) {
        log.info("*** in save, teacher = {}", teacher);
        String query = "INSERT INTO teachers (name) VALUES ($1) RETURNING id";

        return client.preparedQuery(query).execute(Tuple.of(teacher.getName())).map(rows -> {
            Row row = rows.iterator().next();
            teacher.setId(row.getLong("id"));
            return teacher;
        });
    }

    @Override
    public Future<List<Teacher>> findAll() {
        log.info("*** in findAll");
        String query = "SELECT id, name FROM teachers";

        return client.query(query).execute().map(rows -> {
            List<Teacher> teachers = new ArrayList<>();
            for (Row row : rows) {
                teachers.add(Teacher.builder()
                        .id(row.getLong("id"))
                        .name(row.getString("name"))
                        .build());
            }
            return teachers;
        });
    }

    @Override
    public Future<Optional<Teacher>> findById(Long id) {
        log.info("*** in findById, id = {}", id);
        String query = "SELECT id, name FROM teachers WHERE id = $1";

        return client.preparedQuery(query).execute(Tuple.of(id))
                .map(rows -> {
                    if (rows.rowCount() == 0) {
                        return Optional.empty();
                    }
                    Row row = rows.iterator().next();
                    return Optional.of(Teacher.builder()
                            .id(row.getLong("id"))
                            .name(row.getString("name"))
                            .build());
                });
    }

    @Override
    public Future<Teacher> update(Teacher teacher) {
        log.info("*** in update, teacher = {}", teacher);
        StringBuilder queryBuilder = new StringBuilder("UPDATE teachers SET ");
        Tuple params = Tuple.tuple();
        int paramIndex = 1;

        if (teacher.getName() != null) {
            queryBuilder.append("name = $").append(paramIndex++).append(", ");
            params.addString(teacher.getName());
        }

        queryBuilder.setLength(queryBuilder.length() - 2);

        queryBuilder.append(" WHERE id = $").append(paramIndex);
        params.addLong(teacher.getId());

        String query = queryBuilder.toString();

        return client.preparedQuery(query)
                .execute(params)
                .compose(result -> {
                    if (result.rowCount() > 0) {
                        return Future.succeededFuture(teacher);
                    } else {
                        return Future.failedFuture("No rows were updated");
                    }
                });
    }

    @Override
    public Future<Void> deleteById(Long id) {
        log.info("*** in deleteById, id = {}", id);
        String query = "DELETE FROM teachers WHERE id = $1";
        return client.preparedQuery(query).execute(Tuple.of(id)).mapEmpty();
    }

    @Override
    public Future<Boolean> existsById(Long id) {
        log.info("*** in existsById, id = {}", id);

        String query = "SELECT EXISTS (SELECT 1 FROM teachers WHERE id = $1)";
        return client
                .preparedQuery(query)
                .execute(Tuple.of(id))
                .map(rowSet -> {
                    Row row = rowSet.iterator().next();
                    return row.getBoolean(0);
                });
    }

    @Override
    public Future<List<Teacher>> findAllByIds(List<Long> teacherIds) {
        if (teacherIds == null || teacherIds.isEmpty()) {
            return Future.succeededFuture(Collections.emptyList());
        }

        String placeholders = IntStream.range(0, teacherIds.size())
                .mapToObj(i -> "$" + (i + 1))
                .collect(Collectors.joining(", "));

        String query = "SELECT * FROM teachers WHERE id IN (" + placeholders + ")";

        Object[] idsArray = teacherIds.toArray();

        return client
                .preparedQuery(query)
                .execute(Tuple.wrap(idsArray))
                .map(rows -> {
                    List<Teacher> teachers = new ArrayList<>();
                    for (Row row : rows) {
                        teachers.add(Teacher.builder()
                                .id(row.getLong("id"))
                                .name(row.getString("name"))
                                .build());
                    }
                    return teachers;
                });
    }





}
