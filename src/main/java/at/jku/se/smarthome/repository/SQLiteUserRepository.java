package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.model.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite-backed user repository that persists registered users in a local database.
 */
public class SQLiteUserRepository implements UserRepository {

    private final String databaseUrl;

    /**
     * Creates a repository backed by the given SQLite JDBC URL and initializes the schema.
     *
     * @param databaseUrl the JDBC URL of the SQLite database
     */
    public SQLiteUserRepository(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalArgumentException("Database URL must not be blank");
        }

        this.databaseUrl = databaseUrl;
        initializeSchema();
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users(email, password_hash) VALUES(?, ?)";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getEmail());
            statement.setString(2, user.getPasswordHash());
            statement.executeUpdate();
            return user;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save user", exception);
        }
    }

    @Override
    public User findByEmail(String email) {
        String sql = "SELECT email, password_hash FROM users WHERE email = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new User(
                        resultSet.getString("email"),
                        resultSet.getString("password_hash")
                );
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query user", exception);
        }
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM users";

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.getInt(1);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to count users", exception);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl);
    }

    private void initializeSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    email TEXT PRIMARY KEY,
                    password_hash TEXT NOT NULL
                )
                """;

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize database schema", exception);
        }
    }
}
