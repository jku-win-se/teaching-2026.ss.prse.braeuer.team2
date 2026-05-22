package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.model.User;

import java.util.List;

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

    /**
     * Stores a member invitation for an owner's household.
     *
     * @param ownerEmail the owner's email address
     * @param memberEmail the invited member's email address
     */
    void saveMemberInvitation(String ownerEmail, String memberEmail);

    /**
     * Deletes a member invitation from an owner's household.
     *
     * @param ownerEmail the owner's email address
     * @param memberEmail the member's email address
     */
    void deleteMemberInvitation(String ownerEmail, String memberEmail);

    /**
     * Finds the household owner for an invited member.
     *
     * @param memberEmail the member's email address
     * @return the owner email address, or {@code null} if no active invitation exists
     */
    String findHouseholdOwnerByMemberEmail(String memberEmail);

    /**
     * Lists all invited members of an owner's household.
     *
     * @param ownerEmail the owner's email address
     * @return the invited member email addresses
     */
    List<String> findMemberEmailsByOwnerEmail(String ownerEmail);
}
