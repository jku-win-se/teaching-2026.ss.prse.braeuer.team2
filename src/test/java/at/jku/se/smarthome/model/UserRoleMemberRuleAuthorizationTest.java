package at.jku.se.smarthome.model;

import static org.junit.Assert.assertThrows;

import java.io.IOException;

import org.junit.Test;

@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.AtLeastOneConstructor"
})
public class UserRoleMemberRuleAuthorizationTest {

    @Test
    public void memberCannotCreateRule() throws IOException {
        UserRoleTestSupport.RoleTestContext context = UserRoleTestSupport.createMemberContextWithDevice();

        context.system().loginUser("member@example.com", "password123");

        assertThrows("Members must not create rules", IllegalStateException.class, () -> context.system().createRule(
                "New rule",
                RuleTriggerType.DEVICE_STATE_CHANGE,
                "device-1",
                1.0,
                RuleActionType.SET_DEVICE_STATE,
                "device-1",
                0.0
        ));
    }

    @Test
    public void memberCannotUpdateRule() throws IOException {
        UserRoleTestSupport.RoleTestContext context = UserRoleTestSupport.createMemberContextWithRule();

        context.system().loginUser("member@example.com", "password123");

        assertThrows("Members must not update rules", IllegalStateException.class, () -> context.system().updateRule(
                "rule-1",
                "Changed rule",
                RuleTriggerType.DEVICE_STATE_CHANGE,
                "device-1",
                0.0,
                RuleActionType.SET_DEVICE_STATE,
                "device-1",
                1.0
        ));
    }

    @Test
    public void memberCannotRemoveRule() throws IOException {
        UserRoleTestSupport.RoleTestContext context = UserRoleTestSupport.createMemberContextWithRule();

        context.system().loginUser("member@example.com", "password123");

        assertThrows("Members must not remove rules", IllegalStateException.class,
                () -> context.system().removeRule("rule-1"));
    }
}
