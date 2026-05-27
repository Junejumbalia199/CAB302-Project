package com.formcoach.auth;

/**
 * Database-backed implementation of {@link AuthService}.
 * Delegates persistence to an {@link IUserDAO} and uses {@link PasswordUtil}
 * for PBKDF2 password hashing and verification.
 */
public class DatabaseAuthService implements AuthService {

    private final IUserDAO userDAO;

    /**
     * Constructs a DatabaseAuthService backed by the default {@link SqliteUserDAO}.
     */
    public DatabaseAuthService() {
        this.userDAO = new SqliteUserDAO();
    }

    /**
     * Constructs a DatabaseAuthService backed by the supplied DAO (useful for testing).
     * @param userDAO the data access object to use for user persistence
     */
    public DatabaseAuthService(IUserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /** {@inheritDoc} */
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

        if (!PasswordUtil.verifyPassword(password, user.getPassword())) {
            return AuthResult.failure("Incorrect password.");
        }

        AuthSession.setCurrentUser(user);
        return AuthResult.success("Login successful.");
    }

    /** {@inheritDoc} */
    @Override
    public AuthResult register(UserRegistrationRequest request) {
        if (userDAO.getUserByUsername(request.getUsername()) != null) {
            return AuthResult.failure("Username already exists.");
        }

        if (userDAO.getUserByEmail(request.getEmail()) != null) {
            return AuthResult.failure("Email already registered.");
        }

        String hashedPassword = PasswordUtil.hashPassword(request.getPassword());

        User user = new User(
                request.getUsername(),
                hashedPassword,
                request.getEmail()
        );

        userDAO.addUser(user);
        AuthSession.setCurrentUser(user);
        return AuthResult.success("Account created successfully.");
    }
}
