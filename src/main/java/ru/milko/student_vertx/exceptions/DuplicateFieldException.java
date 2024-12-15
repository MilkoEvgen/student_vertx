package ru.milko.student_vertx.exceptions;

public class DuplicateFieldException extends RuntimeException{
    public DuplicateFieldException(String message) {
        super(message);
    }
}
