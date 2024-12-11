CREATE TABLE course_student
(
    id   BIGSERIAL PRIMARY KEY,
    course_id  BIGINT REFERENCES courses (id) ON DELETE CASCADE,
    student_id BIGINT REFERENCES students (id) ON DELETE CASCADE,
    UNIQUE (course_id, student_id)
);