package com.formcoach.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthSessionTest {

    @BeforeEach
    void setUp() {
        AuthSession.clear();
    }

    @Test
    void setCurrentUserShouldStoreUser() {
        User user = new User("nick", "password123", "nick@example.com");

        AuthSession.setCurrentUser(user);

        assertEquals(user, AuthSession.getCurrentUser());
    }

    @Test
    void isLoggedInShouldReturnFalseWhenNoUserIsSet() {
        assertFalse(AuthSession.isLoggedIn());
    }

    @Test
    void isLoggedInShouldReturnTrueWhenUserIsSet() {
        User user = new User("nick", "password123", "nick@example.com");

        AuthSession.setCurrentUser(user);

        assertTrue(AuthSession.isLoggedIn());
    }

    @Test
    void clearShouldRemoveCurrentUser() {
        User user = new User("nick", "password123", "nick@example.com");
        AuthSession.setCurrentUser(user);

        AuthSession.clear();

        assertNull(AuthSession.getCurrentUser());
        assertFalse(AuthSession.isLoggedIn());
    }

    @Test
    void setCurrentUserCanReplaceExistingUser() {
        User firstUser = new User("nick", "password123", "nick@example.com");
        User secondUser = new User("june", "password456", "june@example.com");

        AuthSession.setCurrentUser(firstUser);
        AuthSession.setCurrentUser(secondUser);

        assertEquals(secondUser, AuthSession.getCurrentUser());
        assertTrue(AuthSession.isLoggedIn());
    }
}