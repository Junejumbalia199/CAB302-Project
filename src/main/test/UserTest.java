//package com.formcoach.auth;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {
    @Test
    void testConstructorAndGetters() {
        User user = new User("john_doe", "securePass123", "john@example.com");

        assertEquals("john_doe", user.getUsername());
        assertEquals("securePass123", user.getPassword());
        assertEquals("john@example.com", user.getEmail());
        assertEquals(0, user.getId()); // default int value
    }

    @Test
    void testSetId() {
        User user = new User("john_doe", "securePass123", "john@example.com");

        user.setId(10);
        assertEquals(10, user.getId());
    }

    @Test
    void testSetUsername() {
        User user = new User("john_doe", "securePass123", "john@example.com");

        user.setUsername("jane_doe");
        assertEquals("jane_doe", user.getUsername());
    }

    @Test
    void testSetPassword() {
        User user = new User("john_doe", "securePass123", "john@example.com");

        user.setPassword("newPassword456");
        assertEquals("newPassword456", user.getPassword());
    }

    @Test
    void testSetEmail() {
        User user = new User("john_doe", "securePass123", "john@example.com");

        user.setEmail("jane@example.com");
        assertEquals("jane@example.com", user.getEmail());
    }

    @Test
    void testMultipleSettersTogether() {
        User user = new User("john_doe", "securePass123", "john@example.com");

        user.setId(42);
        user.setUsername("updatedUser");
        user.setPassword("updatedPass");
        user.setEmail("updated@example.com");

        assertAll(
                () -> assertEquals(42, user.getId()),
                () -> assertEquals("updatedUser", user.getUsername()),
                () -> assertEquals("updatedPass", user.getPassword()),
                () -> assertEquals("updated@example.com", user.getEmail())
        );
    }
}
