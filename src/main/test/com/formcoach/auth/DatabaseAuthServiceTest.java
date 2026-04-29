package com.formcoach.auth;

import org.junit.jupiter.api.*;
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
    }

    @Test
    void registerSucceedsForNewUser() {
        AuthResult result = authService.register(
                new UserRegistrationRequest("lebron", "lebrontest@gmail.com", "123456")
        );

        assertTrue(result.isSuccess());
        assertEquals("Account created successfully.", result.getMessage());
        assertNotNull(userDAO.getUserByUsername("lebron"));
    }

    @Test
    void registerFailsWhenUsernameAlreadyExists() {
        userDAO.addUser(new User("lebron", "123456", "lebrontest1@gmail.com"));

        AuthResult result = authService.register(
                new UserRegistrationRequest("lebron", "lebrontest2@gmail.com", "123456")
        );

        assertFalse(result.isSuccess());
        assertEquals("Username already exists.", result.getMessage());
    }

    @Test
    void registerFailsWhenEmailAlreadyExists() {
        userDAO.addUser(new User("lebron", "123456", "lebron@test.com"));

        AuthResult result = authService.register(
                new UserRegistrationRequest("lebron2", "lebron@test.com", "123456")
        );

        assertFalse(result.isSuccess());
        assertEquals("Email already registered.", result.getMessage());
    }

    @Test
    void loginSucceedsWithUsernameAndCorrectPassword() {
        userDAO.addUser(new User("lebron", "123456", "lebron@test.com"));

        AuthResult result = authService.login("lebron", "123456");

        assertTrue(result.isSuccess());
        assertEquals("Login successful.", result.getMessage());
    }

    @Test
    void loginSucceedsWithEmailAndCorrectPassword() {
        userDAO.addUser(new User("lebron", "123456", "lebron@test.com"));

        AuthResult result = authService.login("lebron@test.com", "123456");

        assertTrue(result.isSuccess());
        assertEquals("Login successful.", result.getMessage());
    }

    @Test
    void loginFailsWhenUserDoesNotExist() {
        AuthResult result = authService.login("michael", "123456");

        assertFalse(result.isSuccess());
        assertEquals("No account found with those details.", result.getMessage());
    }

    @Test
    void loginFailsWhenPasswordIsWrong() {
        userDAO.addUser(new User("lebron", "123456", "lebron@test.com"));

        AuthResult result = authService.login("lebron", "111111");

        assertFalse(result.isSuccess());
        assertEquals("Incorrect password.", result.getMessage());
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
                if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                    return true;
                }
            }
            return false;
        }
    }
}