package com.formcoach.auth;

public class AuthResult {
    private final boolean success;
    private final String message;

    public AuthResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public static AuthResult success(String message) {
        return new AuthResult(true, message);
    }

    public static AuthResult failure(String message) {
        return new AuthResult(false, message);
    }
}