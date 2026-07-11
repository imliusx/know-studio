package know.studio.arag.identity.domain;

import know.studio.arag.identity.api.SystemRole;
import know.studio.arag.identity.api.WorkspaceRole;
import know.studio.arag.platform.core.exception.ForbiddenException;
import know.studio.arag.platform.core.exception.UnauthorizedException;
import know.studio.arag.platform.core.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityServiceTest {

    @Test
    void registersNormalizedEmailAndCreatesLoginSession() {
        Fixture fixture = new Fixture();

        AuthSession session = fixture.service.register(" User@Example.COM ", "Shaun", "password123");

        assertThat(session.user().email()).isEqualTo("user@example.com");
        assertThat(session.tokenValue()).isEqualTo("token-" + session.user().userId());
        assertThat(fixture.repository.findUserByEmail("user@example.com")).isPresent();
    }

    @Test
    void rejectsInvalidPasswordWithoutCreatingSession() {
        Fixture fixture = new Fixture();
        fixture.service.register("user@example.com", "Shaun", "password123");
        fixture.session.logout();

        assertThatThrownBy(() -> fixture.service.login("user@example.com", "wrong-password"))
                .isInstanceOf(UnauthorizedException.class);
        assertThat(fixture.session.isLoggedIn()).isFalse();
    }

    @Test
    void workspaceCreatorBecomesOwner() {
        Fixture fixture = new Fixture();
        fixture.service.register("owner@example.com", "Owner", "password123");

        long workspaceId = fixture.service.createWorkspace("Knowledge", "Workspace");

        assertThat(fixture.service.requireRole(workspaceId, WorkspaceRole.OWNER))
                .isEqualTo(WorkspaceRole.OWNER);
    }

    @Test
    void nonMemberCannotReadAnotherWorkspace() {
        Fixture fixture = new Fixture();
        AuthSession owner = fixture.service.register("owner@example.com", "Owner", "password123");
        long workspaceId = fixture.service.createWorkspace("Private", null);
        fixture.session.logout();
        AuthSession outsider = fixture.service.register("outsider@example.com", "Outsider", "password123");

        assertThat(owner.user().userId()).isNotEqualTo(outsider.user().userId());
        assertThatThrownBy(() -> fixture.service.requireWorkspaceReadable(workspaceId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("无权访问");
    }

    @Test
    void memberCannotPerformAdminAction() {
        Fixture fixture = new Fixture();
        AuthSession owner = fixture.service.register("owner@example.com", "Owner", "password123");
        long workspaceId = fixture.service.createWorkspace("Private", null);
        fixture.session.logout();
        AuthSession member = fixture.service.register("member@example.com", "Member", "password123");
        fixture.repository.insertMember(999L, workspaceId, member.user().userId(), WorkspaceRole.MEMBER);

        assertThatThrownBy(() -> fixture.service.requireRole(workspaceId, WorkspaceRole.ADMIN))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("权限不足");
        assertThat(owner.user().systemRole()).isEqualTo(SystemRole.USER);
    }

    @Test
    void listsOnlyCurrentUsersWorkspacesWithRole() {
        Fixture fixture = new Fixture();
        AuthSession owner = fixture.service.register("owner@example.com", "Owner", "password123");
        long ownedWorkspace = fixture.service.createWorkspace("Owned", null);
        fixture.session.logout();
        AuthSession member = fixture.service.register("member@example.com", "Member", "password123");
        fixture.repository.insertMember(999L, ownedWorkspace, member.user().userId(), WorkspaceRole.MEMBER);

        assertThat(fixture.service.listWorkspaces())
                .singleElement()
                .satisfies(workspace -> {
                    assertThat(workspace.workspaceId()).isEqualTo(ownedWorkspace);
                    assertThat(workspace.role()).isEqualTo(WorkspaceRole.MEMBER);
                });
        assertThat(owner.user().userId()).isNotEqualTo(member.user().userId());
    }

    private static final class Fixture {

        private final FakeIdentityRepository repository = new FakeIdentityRepository();
        private final FakeLoginSession session = new FakeLoginSession();
        private final IdentityService service = new IdentityService(
                repository,
                session,
                new PlainPasswordEncoder(),
                new SnowflakeIdGenerator(0, 0)
        );
    }

    private static final class FakeIdentityRepository implements IdentityRepository {

        private final Map<Long, UserAccount> users = new HashMap<>();
        private final Map<String, Long> usersByEmail = new HashMap<>();
        private final Map<Long, Workspace> workspaces = new HashMap<>();
        private final Map<MembershipKey, WorkspaceRole> memberships = new HashMap<>();

        @Override
        public Optional<UserAccount> findUserById(long userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public Optional<UserAccount> findUserByEmail(String email) {
            return Optional.ofNullable(usersByEmail.get(email)).map(users::get);
        }

        @Override
        public void insertUser(UserAccount user) {
            users.put(user.id(), user);
            usersByEmail.put(user.email(), user.id());
        }

        @Override
        public void updateLastLogin(long userId) {
        }

        @Override
        public void insertWorkspace(Workspace workspace) {
            workspaces.put(workspace.id(), workspace);
        }

        @Override
        public void insertMember(long membershipId, long workspaceId, long userId, WorkspaceRole role) {
            memberships.put(new MembershipKey(workspaceId, userId), role);
        }

        @Override
        public Optional<WorkspaceRole> findWorkspaceRole(long workspaceId, long userId) {
            return Optional.ofNullable(memberships.get(new MembershipKey(workspaceId, userId)));
        }

        @Override
        public List<WorkspaceAccess> findWorkspaceAccesses(long userId) {
            return memberships.entrySet().stream()
                    .filter(entry -> entry.getKey().userId() == userId)
                    .map(entry -> new WorkspaceAccess(
                            workspaces.get(entry.getKey().workspaceId()),
                            entry.getValue()
                    ))
                    .toList();
        }

        @Override
        public List<Workspace> findActiveWorkspaces() {
            return List.copyOf(workspaces.values());
        }

        @Override
        public List<WorkspaceMember> findWorkspaceMembers(long workspaceId) {
            return memberships.entrySet().stream()
                    .filter(entry -> entry.getKey().workspaceId() == workspaceId)
                    .map(entry -> new WorkspaceMember(
                            users.get(entry.getKey().userId()),
                            entry.getValue()
                    ))
                    .toList();
        }

        private record MembershipKey(long workspaceId, long userId) {
        }
    }

    private static final class FakeLoginSession implements LoginSession {

        private Long userId;

        @Override
        public void login(long userId) {
            this.userId = userId;
        }

        @Override
        public void logout() {
            userId = null;
        }

        @Override
        public boolean isLoggedIn() {
            return userId != null;
        }

        @Override
        public long currentUserId() {
            return userId;
        }

        @Override
        public String tokenName() {
            return "Authorization";
        }

        @Override
        public String tokenValue() {
            return "token-" + userId;
        }
    }

    private static final class PlainPasswordEncoder implements PasswordEncoder {

        @Override
        public String encode(CharSequence rawPassword) {
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encode(rawPassword).equals(encodedPassword);
        }
    }
}
