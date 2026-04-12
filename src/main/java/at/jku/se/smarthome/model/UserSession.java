package at.jku.se.smarthome.model;

/**
 * Represents the current authenticated user session.
 */
public class UserSession {
    private User currentUser;

    /**
     * Starts a session for the given user.
     *
     * @param user the authenticated user
     */
    public void login(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        currentUser = user;
    }

    /**
     * Ends the current user session.
     */
    public void logout() {
        currentUser = null;
    }

    /**
     * Returns whether a user is currently authenticated.
     *
     * @return {@code true} if a user is logged in, otherwise {@code false}
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Returns the currently authenticated user.
     *
     * @return the current user, or {@code null} if no user is logged in
     */
    public User getCurrentUser() {
        return currentUser;
    }
}
