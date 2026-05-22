package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.model.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory user repository used for fast tests and non-persistent scenarios.
 */
@SuppressWarnings("PMD")
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, String> invitedMembersByEmail = new LinkedHashMap<>();

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

    @Override
    public void saveMemberInvitation(String ownerEmail, String memberEmail) {
        invitedMembersByEmail.put(memberEmail, ownerEmail);
    }

    @Override
    public void deleteMemberInvitation(String ownerEmail, String memberEmail) {
        if (ownerEmail.equals(invitedMembersByEmail.get(memberEmail))) {
            invitedMembersByEmail.remove(memberEmail);
        }
    }

    @Override
    public String findHouseholdOwnerByMemberEmail(String memberEmail) {
        return invitedMembersByEmail.get(memberEmail);
    }

    @Override
    public List<String> findMemberEmailsByOwnerEmail(String ownerEmail) {
        List<String> memberEmails = new ArrayList<>();
        for (Map.Entry<String, String> invitation : invitedMembersByEmail.entrySet()) {
            if (ownerEmail.equals(invitation.getValue())) {
                memberEmails.add(invitation.getKey());
            }
        }
        return memberEmails;
    }
}
