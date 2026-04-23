package com.formcoach.auth;

import java.util.List;

public interface IUserDAO {
    void addUser(User user);
    void updateUser(User user);
    void deleteUser(User user);
    User getUser(int id);
    User getUserByUsername(String username);
    User getUserByEmail(String email);
    List<User> getAllUsers();
    boolean validateLogin(String username, String password);
}
