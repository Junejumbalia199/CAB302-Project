package com.formcoach.auth;

/**
 * Model class representing a registered FormCoach user account.
 * The password field stores the PBKDF2 hash produced by {@link PasswordUtil}, not the plain-text value.
 */
public class User {
    private int id;
    private String username;
    private String password;
    private String email;

    /**
     * Constructs a new User with the given credentials.
     * @param username the unique username
     * @param password the PBKDF2-hashed password
     * @param email    the user's email address
     */
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    /**
     * Returns the user's database-assigned id.
     * @return the primary key id, or 0 if not yet persisted
     */
    public int getId() { return id; }

    /**
     * Sets the user's database id (called by the DAO after insertion).
     * @param id the generated primary key
     */
    public void setId(int id) { this.id = id; }

    /**
     * Returns the user's username.
     * @return the username
     */
    public String getUsername() { return username; }

    /**
     * Sets the user's username.
     * @param username the new username
     */
    public void setUsername(String username) { this.username = username; }

    /**
     * Returns the stored password hash.
     * @return the PBKDF2 password hash
     */
    public String getPassword() { return password; }

    /**
     * Sets the stored password hash.
     * @param password the new PBKDF2 password hash
     */
    public void setPassword(String password) { this.password = password; }

    /**
     * Returns the user's email address.
     * @return the email address
     */
    public String getEmail() { return email; }

    /**
     * Sets the user's email address.
     * @param email the new email address
     */
    public void setEmail(String email) { this.email = email; }
}
