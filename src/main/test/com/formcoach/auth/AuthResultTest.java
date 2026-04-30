package com.formcoach.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthResultTest {

    @Test
    void constructorShouldStoreSuccessAndMessage() {
        AuthResult result = new AuthResult(true, "Operation successful");

        assertTrue(result.isSuccess());
        assertEquals("Operation successful", result.getMessage());
    }

    @Test
    void successFactoryMethodShouldCreateSuccessfulResult() {
        AuthResult result = AuthResult.success("Login successful");

        assertTrue(result.isSuccess());
        assertEquals("Login successful", result.getMessage());
    }

    @Test
    void failureFactoryMethodShouldCreateFailedResult() {
        AuthResult result = AuthResult.failure("Incorrect password");

        assertFalse(result.isSuccess());
        assertEquals("Incorrect password", result.getMessage());
    }

    @Test
    void constructorShouldAllowFalseSuccessValue() {
        AuthResult result = new AuthResult(false, "Something went wrong");

        assertFalse(result.isSuccess());
        assertEquals("Something went wrong", result.getMessage());
    }

    @Test
    void constructorShouldAllowNullMessageIfNoValidationExists() {
        AuthResult result = new AuthResult(true, null);

        assertTrue(result.isSuccess());
        assertNull(result.getMessage());
    }
}