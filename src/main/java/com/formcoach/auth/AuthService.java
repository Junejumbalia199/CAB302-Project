package com.formcoach.auth;

public interface AuthService {
    AuthResult login(String usernameOrEmail, String password);
    AuthResult register(UserRegistrationRequest request);
}