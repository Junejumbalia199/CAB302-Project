package com.formcoach.textoutputgen;

public class PoseValidationException extends RuntimeException {

    private final String errorCode;

    public PoseValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}