package at.jku.se.smarthome.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for hashing and verifying user passwords with bcrypt.
 */
public final class PasswordHasher {

    private static final int WORK_FACTOR = 12;

    private PasswordHasher() {
    }

    /**
     * Hashes a plain text password using bcrypt.
     *
     * @param password the plain text password
     * @return the generated bcrypt hash
     */
    public static String hash(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }

        return BCrypt.hashpw(password, BCrypt.gensalt(WORK_FACTOR));
    }

    /**
     * Verifies a plain text password against a bcrypt hash.
     *
     * @param password the plain text password
     * @param passwordHash the stored bcrypt hash
     * @return {@code true} if the password matches the hash, otherwise {@code false}
     */
    public static boolean verify(String password, String passwordHash) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash must not be blank");
        }

        return BCrypt.checkpw(password, passwordHash);
    }
}
