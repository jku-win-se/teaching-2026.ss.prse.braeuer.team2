package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.model.User;

/**
 * Defines persistence operations for registered users.
 */
public interface UserRepository {
    /**
     * Stores a user in the backing data source.
     *
     * @param user the user to store
     * @return the stored user
     */
    User save(User user);

    /**
     * Finds a user by email address.
     *
     * @param email the email address to search for
     * @return the matching user, or {@code null} if none exists
     */
    User findByEmail(String email);

    /**
     * Returns the number of stored users.
     *
     * @return the number of users
     */
    int count();
}
