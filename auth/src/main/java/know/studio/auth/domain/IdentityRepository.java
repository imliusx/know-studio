package know.studio.auth.domain;

import know.studio.auth.api.TeamRole;

import java.util.List;
import java.util.Optional;

public interface IdentityRepository {

    Optional<UserAccount> findUserById(long userId);

    Optional<UserAccount> findUserByEmail(String email);

    void insertUser(UserAccount user);

    void updateLastLogin(long userId);

    void insertTeam(Team team);

    default boolean updateTeam(long teamId, String name, String description, Long parentId) {
        return false;
    }

    default boolean deactivateTeam(long teamId) {
        return false;
    }

    void insertTeamMember(long membershipId, long teamId, long userId, TeamRole role);

    default boolean updateTeamMemberRole(long teamId, long userId, TeamRole role) {
        return false;
    }

    default boolean deleteTeamMember(long teamId, long userId) {
        return false;
    }

    Optional<TeamRole> findTeamRole(long teamId, long userId);

    List<TeamAccess> findTeamAccesses(long userId);

    List<Team> findActiveTeams();

    List<TeamMember> findTeamMembers(long teamId);
}
