package com.formcoach.auth;

import java.util.List;

/**
 * Data access object interface for {@link User} persistence operations.
 * Implementations manage CRUD operations against the underlying user store.
 */
public interface IUserDAO {

    /**
     * Persists a new user and populates its generated id.
     * @param user the user to add; {@code user.getId()} will be set after insertion
     */
    void addUser(User user);

    /**
     * Updates an existing user record matched by id.
     * @param user the user containing updated field values and a valid id
     */
    void updateUser(User user);

    /**
     * Deletes the user record matched by id.
     * @param user the user to delete; only the id field is used
     */
    void deleteUser(User user);

    /**
     * Retrieves a user by their numeric id.
     * @param id the user's primary key
     * @return the matching {@link User}, or {@code null} if not found
     */
    User getUser(int id);

    /**
     * Retrieves a user by their username.
     * @param username the username to search for
     * @return the matching {@link User}, or {@code null} if not found
     */
    User getUserByUsername(String username);

    /**
     * Retrieves a user by their email address.
     * @param email the email address to search for
     * @return the matching {@link User}, or {@code null} if not found
     */
    User getUserByEmail(String email);

    /**
     * Returns all users in the data store.
     * @return a list of all {@link User} records; empty list if none exist
     */
    List<User> getAllUsers();

    /**
     * Validates a username/password pair against stored credentials.
     * @param username the username to check
     * @param password the plain-text password to verify
     * @return {@code true} if the credentials match a stored record
     */
    boolean validateLogin(String username, String password);
}
