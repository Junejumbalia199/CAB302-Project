package com.formcoach.auth;

/**
 * Data transfer object carrying the credentials needed to register a new user account.
 */
public class UserRegistrationRequest {
    private final String username;
    private final String email;
    private final String password;

    /**
     * Constructs a new UserRegistrationRequest with the supplied credentials.
     * @param username the desired username
     * @param email    the user's email address
     * @param password the plain-text password (will be hashed before storage)
     */
    public UserRegistrationRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    /**
     * Returns the requested username.
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the user's email address.
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the plain-text password supplied at construction time.
     * @return the plain-text password
     */
    public String getPassword() {
        return password;
    }
}
