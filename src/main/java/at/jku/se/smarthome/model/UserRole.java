package at.jku.se.smarthome.model;

import java.util.Locale;

/**
 * Defines the supported permission roles for registered users.
 */
public enum UserRole {
    OWNER,
    MEMBER;

    /**
     * Parses a persisted role value.
     *
     * @param value the stored role value
     * @return the parsed role, or {@link #OWNER} for missing values
     */
    public static UserRole fromStorageValue(String value) {
        if (value == null || value.isBlank()) {
            return OWNER;
        }
        return UserRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
