package know.studio.arag.identity.domain;

import know.studio.arag.identity.api.SystemRole;
import know.studio.arag.identity.api.TeamRole;
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
    void systemAdminCreatesTeamAndBecomesTeamAdmin() {
        Fixture fixture = new Fixture();
        AuthSession admin = fixture.service.register("admin@example.com", "Admin", "password123");
        fixture.repository.promote(admin.user().userId());

        long teamId = fixture.service.createTeam("Engineering", null, null);

        assertThat(fixture.service.requireTeamRole(teamId, TeamRole.TEAM_ADMIN))
                .isEqualTo(TeamRole.TEAM_ADMIN);
        assertThat(fixture.service.listTeams())
                .extracting(team -> team.teamId())
                .contains(teamId);
    }

    @Test
    void regularUserCannotCreateTeam() {
        Fixture fixture = new Fixture();
        fixture.service.register("user@example.com", "User", "password123");

        assertThatThrownBy(() -> fixture.service.createTeam("Engineering", null, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("系统管理员");
    }

    @Test
    void teamMemberCannotPerformTeamAdminAction() {
        Fixture fixture = new Fixture();
        AuthSession admin = fixture.service.register("admin@example.com", "Admin", "password123");
        fixture.repository.promote(admin.user().userId());
        long teamId = fixture.service.createTeam("Engineering", null, null);
        fixture.session.logout();
        AuthSession member = fixture.service.register("member@example.com", "Member", "password123");
        fixture.repository.insertTeamMember(1000L, teamId, member.user().userId(), TeamRole.MEMBER);

        assertThatThrownBy(() -> fixture.service.requireTeamRole(teamId, TeamRole.TEAM_ADMIN))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("权限不足");
        assertThat(fixture.service.currentUserTeamIds()).containsExactly(teamId);
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
        private final Map<Long, Team> teams = new HashMap<>();
        private final Map<TeamMembershipKey, TeamRole> teamMemberships = new HashMap<>();

        private void promote(long userId) {
            UserAccount user = users.get(userId);
            users.put(userId, new UserAccount(
                    user.id(),
                    user.email(),
                    user.displayName(),
                    user.passwordHash(),
                    SystemRole.ADMIN,
                    user.status()
            ));
        }

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
        public void insertTeam(Team team) {
            teams.put(team.id(), team);
        }

        @Override
        public void insertTeamMember(long membershipId, long teamId, long userId, TeamRole role) {
            teamMemberships.put(new TeamMembershipKey(teamId, userId), role);
        }

        @Override
        public Optional<TeamRole> findTeamRole(long teamId, long userId) {
            return Optional.ofNullable(teamMemberships.get(new TeamMembershipKey(teamId, userId)));
        }

        @Override
        public List<TeamAccess> findTeamAccesses(long userId) {
            return teamMemberships.entrySet().stream()
                    .filter(entry -> entry.getKey().userId() == userId)
                    .map(entry -> new TeamAccess(teams.get(entry.getKey().teamId()), entry.getValue()))
                    .toList();
        }

        @Override
        public List<Team> findActiveTeams() {
            return List.copyOf(teams.values());
        }

        @Override
        public List<TeamMember> findTeamMembers(long teamId) {
            return teamMemberships.entrySet().stream()
                    .filter(entry -> entry.getKey().teamId() == teamId)
                    .map(entry -> new TeamMember(users.get(entry.getKey().userId()), entry.getValue()))
                    .toList();
        }

        private record TeamMembershipKey(long teamId, long userId) {
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
