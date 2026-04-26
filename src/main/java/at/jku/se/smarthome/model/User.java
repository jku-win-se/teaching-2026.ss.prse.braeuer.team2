package at.jku.se.smarthome.model;

/**
 * Represents a registered user account with a unique email address and hashed password.
 */
public class User {
    private final String email;
    private final String passwordHash;
    private final UserRole role;

    /**
     * Creates a new user.
     *
     * @param email the unique email address of the user
     * @param passwordHash the hashed password of the user
     */
    public User(String email, String passwordHash) {
        this(email, passwordHash, UserRole.OWNER);
    }

    /**
     * Creates a new user with the provided role.
     *
     * @param email the unique email address of the user
     * @param passwordHash the hashed password of the user
     * @param role the permission role of the user
     */
    public User(String email, String passwordHash, UserRole role) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash must not be blank");
        }
        if (role == null) {
            throw new IllegalArgumentException("User role must not be null");
        }

        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
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

    /**
     * Returns the permission role of the user.
     *
     * @return the user role
     */
    public UserRole getRole() {
        return role;
    }
}
