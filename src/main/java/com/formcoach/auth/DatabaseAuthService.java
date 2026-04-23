package com.formcoach.auth;

public class DatabaseAuthService implements AuthService {

    private final IUserDAO userDAO;

    public DatabaseAuthService() {
        this.userDAO = new SqliteUserDAO();
    }

    @Override
    public AuthResult login(String usernameOrEmail, String password) {
        User user;

        if (usernameOrEmail.contains("@")) {
            user = userDAO.getUserByEmail(usernameOrEmail);
        } else {
            user = userDAO.getUserByUsername(usernameOrEmail);
        }

        if (user == null) {
            return AuthResult.failure("No account found with those details.");
        }

        if (!user.getPassword().equals(password)) {
            return AuthResult.failure("Incorrect password.");
        }

        AuthSession.setCurrentUser(user);
        return AuthResult.success("Login successful.");
    }

    @Override
    public AuthResult register(UserRegistrationRequest request) {
        if (userDAO.getUserByUsername(request.getUsername()) != null) {
            return AuthResult.failure("Username already exists.");
        }

        if (userDAO.getUserByEmail(request.getEmail()) != null) {
            return AuthResult.failure("Email already registered.");
        }

        User user = new User(
                request.getUsername(),
                request.getPassword(),
                request.getEmail()
        );

        userDAO.addUser(user);
        AuthSession.setCurrentUser(user);
        return AuthResult.success("Account created successfully.");
    }
}