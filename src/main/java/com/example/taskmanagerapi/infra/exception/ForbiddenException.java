package com.example.taskmanagerapi.infra.exception;

public class ForbiddenException extends RuntimeException {

    private final String code;

    public ForbiddenException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ForbiddenException(String message) {
        this("FORBIDDEN", message);
    }

    public String getCode() {
        return code;
    }
}
