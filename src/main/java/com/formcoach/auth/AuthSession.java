package com.formcoach.auth;

public final class AuthSession {
    private static User currentUser;

    private AuthSession() {
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void clear()  {currentUser = null;}
}