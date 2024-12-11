package ru.milko.student_vertx.repository.impl;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.extern.slf4j.Slf4j;
import ru.milko.student_vertx.model.Course;
import ru.milko.student_vertx.model.Department;
import ru.milko.student_vertx.repository.DepartmentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class DepartmentRepositoryImpl implements DepartmentRepository {
    private final Pool client;

    public DepartmentRepositoryImpl(Pool client) {
        this.client = client;
    }

    @Override
    public Future<Department> save(Department department) {
        log.info("*** in save, department = {}", department);
        String query = "INSERT INTO departments (name) VALUES ($1) RETURNING id";

        return client.preparedQuery(query).execute(Tuple.of(department.getName())).map(rows -> {
            Row row = rows.iterator().next();
            department.setId(row.getLong("id"));
            return department;
        });
    }

    @Override
    public Future<List<Department>> findAll() {
        log.info("*** in findAll");
        String query = "SELECT id, name, head_of_department_id FROM departments";

        return client.query(query).execute().map(rows -> {
            List<Department> departments = new ArrayList<>();
            for (Row row : rows) {
                departments.add(Department.builder()
                        .id(row.getLong("id"))
                        .name(row.getString("name"))
                        .headOfDepartmentId(row.getLong("head_of_department_id"))
                        .build());
            }
            return departments;
        });
    }

    @Override
    public Future<List<Department>> findAllByHeadIds(List<Long> teacherIds) {
        log.info("*** in findAllByHeadIds, teacherIds = {}", teacherIds);
        String query = "SELECT id, name, head_of_department_id FROM departments WHERE head_of_department_id = ANY($1)";

        Long[] teacherIdsArray = teacherIds.toArray(new Long[0]);
        return client.preparedQuery(query)
                .execute(Tuple.of((Object) teacherIdsArray))
                .map(rows -> {
                    List<Department> departments = new ArrayList<>();
                    for (Row row : rows) {
                        departments.add(Department.builder()
                                .id(row.getLong("id"))
                                .name(row.getString("name"))
                                .headOfDepartmentId(row.getLong("head_of_department_id"))
                                .build());
                    }
                    return departments;
                });
    }

    @Override
    public Future<Optional<Department>> findById(Long id) {
        log.info("*** in findById, id = {}", id);
        String query = "SELECT id, name, head_of_department_id FROM departments WHERE id = $1";

        return client.preparedQuery(query).execute(Tuple.of(id))
                .map(rows -> {
                    if (rows.rowCount() == 0) {
                        return Optional.empty();
                    }
                    Row row = rows.iterator().next();
                    return Optional.of(Department.builder()
                            .id(row.getLong("id"))
                            .name(row.getString("name"))
                            .headOfDepartmentId(row.getLong("head_of_department_id"))
                            .build());
                });
    }

    @Override
    public Future<Optional<Department>> findByHeadOfDepartmentId(Long headOfDepartmentId) {
        log.info("*** in findByHeadOfDepartmentId, headOfDepartmentId = {}", headOfDepartmentId);
        String query = "SELECT id, name FROM departments WHERE head_of_department_id = $1";

        return client.preparedQuery(query).execute(Tuple.of(headOfDepartmentId))
                .map(rows -> {
                    if (rows.rowCount() == 0) {
                        return Optional.empty();
                    }
                    Row row = rows.iterator().next();
                    return Optional.of(Department.builder()
                            .id(row.getLong("id"))
                            .name(row.getString("name"))
                            .build());
                });
    }

    @Override
    public Future<Department> update(Department department) {
        log.info("*** in update, department = {}", department);
        StringBuilder queryBuilder = new StringBuilder("UPDATE departments SET ");
        Tuple params = Tuple.tuple();
        int paramIndex = 1;

        if (department.getName() != null) {
            queryBuilder.append("name = $").append(paramIndex++).append(", ");
            params.addString(department.getName());
        }

        queryBuilder.setLength(queryBuilder.length() - 2);

        queryBuilder.append(" WHERE id = $").append(paramIndex);
        params.addLong(department.getId());

        String query = queryBuilder.toString();

        return client.preparedQuery(query)
                .execute(params)
                .compose(result -> {
                    if (result.rowCount() > 0) {
                        return Future.succeededFuture(department);
                    } else {
                        return Future.failedFuture("No rows were updated");
                    }
                });
    }

    @Override
    public Future<Void> deleteById(Long id) {
        log.info("*** in deleteById, id = {}", id);
        String query = "DELETE FROM departments WHERE id = $1";
        return client.preparedQuery(query).execute(Tuple.of(id)).mapEmpty();
    }

    @Override
    public Future<Boolean> existsById(Long id) {
        log.info("*** in existsById, id = {}", id);

        String sql = "SELECT EXISTS (SELECT 1 FROM departments WHERE id = $1)";
        return client
                .preparedQuery(sql)
                .execute(Tuple.of(id))
                .map(rowSet -> {
                    Row row = rowSet.iterator().next();
                    return row.getBoolean(0);
                });
    }

    @Override
    public Future<Void> setTeacherToDepartment(Long departmentId, Long teacherId) {
        log.info("*** in setTeacherToDepartment, departmentId = {}, teacherId = {}", departmentId, teacherId);
        String query = "UPDATE departments SET head_of_department_id = $1 WHERE id = $2";

        return client.preparedQuery(query)
                .execute(Tuple.of(teacherId, departmentId))
                .map(rows -> null);
    }
}
