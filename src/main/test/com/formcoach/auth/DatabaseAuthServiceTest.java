package com.formcoach.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseAuthServiceTest {

    private FakeUserDAO userDAO;
    private DatabaseAuthService authService;

    @BeforeEach
    void setUp() {
        userDAO = new FakeUserDAO();
        authService = new DatabaseAuthService(userDAO);
        AuthSession.clear();
    }

    @Test
    void registerSucceedsForNewUser() {
        AuthResult result = authService.register(
                new UserRegistrationRequest("lebron", "lebrontest@gmail.com", "Password123!")
        );

        assertTrue(result.isSuccess());
        assertEquals("Account created successfully.", result.getMessage());

        User storedUser = userDAO.getUserByUsername("lebron");
        assertNotNull(storedUser);
        assertEquals("lebrontest@gmail.com", storedUser.getEmail());

        // Confirm password was stored hashed, not plain text
        assertNotEquals("Password123!", storedUser.getPassword());
        assertTrue(PasswordUtil.verifyPassword("Password123!", storedUser.getPassword()));
    }

    @Test
    void registerFailsWhenUsernameAlreadyExists() {
        userDAO.addUser(new User(
                "lebron",
                PasswordUtil.hashPassword("Password123!"),
                "lebrontest1@gmail.com"
        ));

        AuthResult result = authService.register(
                new UserRegistrationRequest("lebron", "lebrontest2@gmail.com", "Password123!")
        );

        assertFalse(result.isSuccess());
        assertEquals("Username already exists.", result.getMessage());
    }

    @Test
    void registerFailsWhenEmailAlreadyExists() {
        userDAO.addUser(new User(
                "lebron",
                PasswordUtil.hashPassword("Password123!"),
                "lebron@test.com"
        ));

        AuthResult result = authService.register(
                new UserRegistrationRequest("lebron2", "lebron@test.com", "Password123!")
        );

        assertFalse(result.isSuccess());
        assertEquals("Email already registered.", result.getMessage());
    }

    @Test
    void loginSucceedsWithUsernameAndCorrectPassword() {
        userDAO.addUser(new User(
                "lebron",
                PasswordUtil.hashPassword("Password123!"),
                "lebron@test.com"
        ));

        AuthResult result = authService.login("lebron", "Password123!");

        assertTrue(result.isSuccess());
        assertEquals("Login successful.", result.getMessage());
        assertNotNull(AuthSession.getCurrentUser());
        assertEquals("lebron", AuthSession.getCurrentUser().getUsername());
    }

    @Test
    void loginSucceedsWithEmailAndCorrectPassword() {
        userDAO.addUser(new User(
                "lebron",
                PasswordUtil.hashPassword("Password123!"),
                "lebron@test.com"
        ));

        AuthResult result = authService.login("lebron@test.com", "Password123!");

        assertTrue(result.isSuccess());
        assertEquals("Login successful.", result.getMessage());
        assertNotNull(AuthSession.getCurrentUser());
        assertEquals("lebron@test.com", AuthSession.getCurrentUser().getEmail());
    }

    @Test
    void loginFailsWhenUserDoesNotExist() {
        AuthResult result = authService.login("michael", "Password123!");

        assertFalse(result.isSuccess());
        assertEquals("No account found with those details.", result.getMessage());
        assertNull(AuthSession.getCurrentUser());
    }

    @Test
    void loginFailsWhenPasswordIsWrong() {
        userDAO.addUser(new User(
                "lebron",
                PasswordUtil.hashPassword("Password123!"),
                "lebron@test.com"
        ));

        AuthResult result = authService.login("lebron", "WrongPassword1!");

        assertFalse(result.isSuccess());
        assertEquals("Incorrect password.", result.getMessage());
        assertNull(AuthSession.getCurrentUser());
    }

    private static class FakeUserDAO implements IUserDAO {
        private final List<User> users = new ArrayList<>();
        private int nextId = 1;

        @Override
        public void addUser(User user) {
            user.setId(nextId++);
            users.add(user);
        }

        @Override
        public void updateUser(User user) {
            for (int i = 0; i < users.size(); i++) {
                if (users.get(i).getId() == user.getId()) {
                    users.set(i, user);
                    return;
                }
            }
        }

        @Override
        public void deleteUser(User user) {
            users.removeIf(u -> u.getId() == user.getId());
        }

        @Override
        public User getUser(int id) {
            for (User user : users) {
                if (user.getId() == id) {
                    return user;
                }
            }
            return null;
        }

        @Override
        public User getUserByUsername(String username) {
            for (User user : users) {
                if (user.getUsername().equals(username)) {
                    return user;
                }
            }
            return null;
        }

        @Override
        public User getUserByEmail(String email) {
            for (User user : users) {
                if (user.getEmail().equals(email)) {
                    return user;
                }
            }
            return null;
        }

        @Override
        public List<User> getAllUsers() {
            return new ArrayList<>(users);
        }

        @Override
        public boolean validateLogin(String username, String password) {
            for (User user : users) {
                if (user.getUsername().equals(username)
                        && PasswordUtil.verifyPassword(password, user.getPassword())) {
                    return true;
                }
            }
            return false;
        }
    }
}