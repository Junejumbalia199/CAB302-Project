package com.formcoach.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for hashing and verifying passwords using PBKDF2 with HMAC-SHA256.
 * A random 16-byte salt is generated for each hash, stored as {@code base64(salt):base64(hash)}.
 */
public final class PasswordUtil {

    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    private PasswordUtil() {
    }

    /**
     * Hashes a plain-text password with a freshly generated random salt.
     * @param password the plain-text password to hash
     * @return a {@code "base64(salt):base64(hash)"} string suitable for storage
     * @throws RuntimeException if the PBKDF2 algorithm is unavailable
     */
    public static String hashPassword(String password) {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_LENGTH
            );

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();

            return Base64.getEncoder().encodeToString(salt) + ":" +
                    Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    /**
     * Verifies a plain-text password against a previously hashed value.
     * @param password    the plain-text password to check
     * @param storedValue the stored {@code "base64(salt):base64(hash)"} string
     * @return {@code true} if the password matches the stored hash
     */
    public static boolean verifyPassword(String password, String storedValue) {
        try {
            String[] parts = storedValue.split(":");
            if (parts.length != 2) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_LENGTH
            );

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] actualHash = factory.generateSecret(spec).getEncoded();

            if (actualHash.length != expectedHash.length) {
                return false;
            }

            for (int i = 0; i < actualHash.length; i++) {
                if (actualHash[i] != expectedHash[i]) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }
}