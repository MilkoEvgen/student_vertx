package ru.milko.student_vertx;

import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Pool;
import lombok.Getter;
import lombok.Setter;
import org.mapstruct.factory.Mappers;
import ru.milko.student_vertx.config.Config;
import ru.milko.student_vertx.mapper.CourseMapper;
import ru.milko.student_vertx.mapper.DepartmentMapper;
import ru.milko.student_vertx.mapper.StudentMapper;
import ru.milko.student_vertx.mapper.TeacherMapper;
import ru.milko.student_vertx.repository.CourseRepository;
import ru.milko.student_vertx.repository.DepartmentRepository;
import ru.milko.student_vertx.repository.StudentRepository;
import ru.milko.student_vertx.repository.TeacherRepository;
import ru.milko.student_vertx.repository.impl.CourseRepositoryImpl;
import ru.milko.student_vertx.repository.impl.DepartmentRepositoryImpl;
import ru.milko.student_vertx.repository.impl.StudentRepositoryImpl;
import ru.milko.student_vertx.repository.impl.TeacherRepositoryImpl;
import ru.milko.student_vertx.rest.BasicController;
import ru.milko.student_vertx.rest.CourseController;
import ru.milko.student_vertx.rest.DepartmentController;
import ru.milko.student_vertx.rest.StudentController;
import ru.milko.student_vertx.rest.TeacherController;
import ru.milko.student_vertx.service.CourseService;
import ru.milko.student_vertx.service.DepartmentService;
import ru.milko.student_vertx.service.StudentService;
import ru.milko.student_vertx.service.TeacherService;
import ru.milko.student_vertx.service.impl.CourseServiceImpl;
import ru.milko.student_vertx.service.impl.DepartmentServiceImpl;
import ru.milko.student_vertx.service.impl.StudentServiceImpl;
import ru.milko.student_vertx.service.impl.TeacherServiceImpl;

import java.util.ArrayList;
import java.util.List;

public class ApplicationContext {
    private final Pool pool;
    private final List<BasicController> controllers = new ArrayList<>();

    public ApplicationContext(Pool pool) {
        this.pool = pool;
    }

    public void initDependencies(){
        final StudentMapper studentMapper = Mappers.getMapper(StudentMapper.class);
        final CourseMapper courseMapper = Mappers.getMapper(CourseMapper.class);
        final TeacherMapper teacherMapper = Mappers.getMapper(TeacherMapper.class);
        final DepartmentMapper departmentMapper = Mappers.getMapper(DepartmentMapper.class);

        final StudentRepository studentRepository = new StudentRepositoryImpl(pool);
        final CourseRepository courseRepository = new CourseRepositoryImpl(pool);
        final TeacherRepository teacherRepository = new TeacherRepositoryImpl(pool);
        final DepartmentRepository departmentRepository = new DepartmentRepositoryImpl(pool);

        final StudentService studentService = new StudentServiceImpl(studentRepository, courseRepository, teacherRepository, studentMapper, courseMapper, teacherMapper);
        final CourseService courseService = new CourseServiceImpl(courseRepository, teacherRepository, studentRepository, courseMapper, teacherMapper, studentMapper);
        final TeacherService teacherService = new TeacherServiceImpl(teacherRepository, courseRepository, departmentRepository, teacherMapper, courseMapper, departmentMapper);
        final DepartmentService departmentService = new DepartmentServiceImpl(departmentRepository, teacherRepository, departmentMapper, teacherMapper);

        final StudentController studentController = new StudentController(studentService);
        final CourseController courseController = new CourseController(courseService);
        final TeacherController teacherController = new TeacherController(teacherService);
        final DepartmentController departmentController = new DepartmentController(departmentService);

        controllers.add(studentController);
        controllers.add(courseController);
        controllers.add(teacherController);
        controllers.add(departmentController);
    }

    public void registerRoutes(Router router){
        for (BasicController controller : controllers) {
            controller.registerRoutes(router);
        }
    }
}
