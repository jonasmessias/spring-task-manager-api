package com.example.taskmanagerapi.infra.exception;

public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this("BAD_REQUEST", message);
    }

    public String getCode() {
        return code;
    }
}
