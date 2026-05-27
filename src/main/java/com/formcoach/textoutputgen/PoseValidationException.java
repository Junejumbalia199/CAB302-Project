package com.formcoach.textoutputgen;

/**
 * Thrown by {@link textoutputgen} when pose data fails validation checks,
 * such as null arrays, NaN values, or an unrecognised exercise type.
 */
public class PoseValidationException extends RuntimeException {

    /** Short identifier for the type of validation failure (e.g. {@code "NULL_INPUT"}). */
    private final String errorCode;

    /**
     * Constructs a new PoseValidationException with a short error code and a detailed message.
     * @param errorCode a short identifier for the failure type (e.g. {@code "NULL_INPUT"})
     * @param message   a human-readable description of the validation failure
     */
    public PoseValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Returns the short error code that identifies the type of validation failure.
     * @return the error code string
     */
    public String getErrorCode() {
        return errorCode;
    }
}
