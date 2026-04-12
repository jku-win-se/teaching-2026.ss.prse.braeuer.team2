package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.model.User;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory user repository used for fast tests and non-persistent scenarios.
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> users = new LinkedHashMap<>();

    @Override
    public User save(User user) {
        users.put(user.getEmail(), user);
        return user;
    }

    @Override
    public User findByEmail(String email) {
        return users.get(email);
    }

    @Override
    public int count() {
        return users.size();
    }
}
