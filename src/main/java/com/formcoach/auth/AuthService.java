package com.formcoach.auth;

/**
 * Service interface for user authentication operations.
 * Implementations are responsible for verifying credentials and creating new accounts.
 */
public interface AuthService {

    /**
     * Attempts to log in with the supplied credentials.
     * @param usernameOrEmail the user's username or email address
     * @param password        the plain-text password to verify
     * @return an {@link AuthResult} indicating success or failure with a descriptive message
     */
    AuthResult login(String usernameOrEmail, String password);

    /**
     * Registers a new user account from the supplied request.
     * @param request the registration details (username, email, password)
     * @return an {@link AuthResult} indicating success or failure with a descriptive message
     */
    AuthResult register(UserRegistrationRequest request);
}
