package com.example.taskmanagerapi.infra.exception;

public class UnauthorizedException extends RuntimeException {

    private final String code;

    public UnauthorizedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public UnauthorizedException(String message) {
        this("UNAUTHORIZED", message);
    }

    public String getCode() {
        return code;
    }
}
