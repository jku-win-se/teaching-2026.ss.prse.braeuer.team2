package at.jku.se.smarthome.model;

/**
 * Represents a registered user account with a unique email address and hashed password.
 */
public class User {
    private final String email;
    private final String passwordHash;

    /**
     * Creates a new user.
     *
     * @param email the unique email address of the user
     * @param passwordHash the hashed password of the user
     */
    public User(String email, String passwordHash) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash must not be blank");
        }

        this.email = email;
        this.passwordHash = passwordHash;
    }

    /**
     * Returns the email address of the user.
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the stored password hash of the user.
     *
     * @return the password hash
     */
    public String getPasswordHash() {
        return passwordHash;
    }
}
