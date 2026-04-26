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
        UserRole role = OWNER;
        if (value != null && !value.isBlank()) {
            role = UserRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
        return role;
    }
}
