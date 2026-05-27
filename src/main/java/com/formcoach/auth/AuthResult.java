package com.formcoach.auth;

/**
 * Immutable result object returned by {@link AuthService} operations.
 * Use {@link #isSuccess()} to check the outcome and {@link #getMessage()} to
 * retrieve a human-readable description suitable for display in the UI.
 */
public class AuthResult {
    private final boolean success;
    private final String message;

    /**
     * Constructs a new AuthResult with the given outcome and message.
     * @param success {@code true} if the operation succeeded
     * @param message a human-readable description of the outcome
     */
    public AuthResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    /**
     * Returns whether the operation succeeded.
     * @return {@code true} if the operation was successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the human-readable outcome message.
     * @return the outcome message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Creates a successful AuthResult with the given message.
     * @param message a human-readable success description
     * @return a successful AuthResult
     */
    public static AuthResult success(String message) {
        return new AuthResult(true, message);
    }

    /**
     * Creates a failed AuthResult with the given message.
     * @param message a human-readable failure description
     * @return a failed AuthResult
     */
    public static AuthResult failure(String message) {
        return new AuthResult(false, message);
    }
}
