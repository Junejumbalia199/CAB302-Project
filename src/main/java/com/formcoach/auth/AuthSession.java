package com.formcoach.auth;

/**
 * Application-wide singleton that tracks the currently authenticated user.
 * Call {@link #setCurrentUser(User)} after a successful login and {@link #clear()} on logout.
 */
public final class AuthSession {
    private static User currentUser;

    private AuthSession() {
    }

    /**
     * Stores the authenticated user for the current session.
     * @param user the user who has just logged in or registered
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /**
     * Returns the currently logged-in user.
     * @return the current {@link User}, or {@code null} if no user is logged in
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Returns whether a user is currently logged in.
     * @return {@code true} if a user session is active
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Clears the current session, effectively logging the user out.
     */
    public static void clear() { currentUser = null; }
}
