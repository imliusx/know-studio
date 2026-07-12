package know.studio.arag.identity.infra.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import know.studio.arag.identity.api.SystemRole;
import know.studio.arag.identity.api.TeamRole;
import know.studio.arag.identity.domain.IdentityRepository;
import know.studio.arag.identity.domain.UserAccount;
import know.studio.arag.identity.domain.UserStatus;
import know.studio.arag.identity.domain.Team;
import know.studio.arag.identity.domain.TeamAccess;
import know.studio.arag.identity.domain.TeamMember;
import know.studio.arag.identity.domain.TeamStatus;
import know.studio.arag.identity.infra.persistence.entity.TeamEntity;
import know.studio.arag.identity.infra.persistence.entity.TeamMemberEntity;
import know.studio.arag.identity.infra.persistence.entity.UserEntity;
import know.studio.arag.identity.infra.persistence.mapper.UserMapper;
import know.studio.arag.identity.infra.persistence.mapper.TeamMapper;
import know.studio.arag.identity.infra.persistence.mapper.TeamMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MybatisIdentityRepository implements IdentityRepository {

    private final UserMapper userMapper;
    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;

    @Override
    public Optional<UserAccount> findUserById(long userId) {
        return Optional.ofNullable(userMapper.selectById(userId)).map(MybatisIdentityRepository::toDomain);
    }

    @Override
    public Optional<UserAccount> findUserByEmail(String email) {
        UserEntity entity = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getEmail, email));
        return Optional.ofNullable(entity).map(MybatisIdentityRepository::toDomain);
    }

    @Override
    public void insertUser(UserAccount user) {
        userMapper.insert(toEntity(user));
    }

    @Override
    public void updateLastLogin(long userId) {
        userMapper.update(Wrappers.<UserEntity>lambdaUpdate()
                .eq(UserEntity::getId, userId)
                .setSql("last_login_at = CURRENT_TIMESTAMP")
                .setSql("updated_at = CURRENT_TIMESTAMP"));
    }

    @Override
    public void insertTeam(Team team) {
        TeamEntity entity = new TeamEntity();
        entity.setId(team.id());
        entity.setName(team.name());
        entity.setDescription(team.description());
        entity.setParentId(team.parentId());
        entity.setCreatedBy(team.createdBy());
        entity.setStatus(team.status().name());
        teamMapper.insert(entity);
    }

    @Override
    public boolean updateTeam(long teamId, String name, String description, Long parentId) {
        return teamMapper.update(Wrappers.<TeamEntity>lambdaUpdate()
                .eq(TeamEntity::getId, teamId)
                .eq(TeamEntity::getStatus, TeamStatus.ACTIVE.name())
                .set(TeamEntity::getName, name)
                .set(TeamEntity::getDescription, description)
                .set(TeamEntity::getParentId, parentId)) == 1;
    }

    @Override
    public boolean deactivateTeam(long teamId) {
        return teamMapper.update(Wrappers.<TeamEntity>lambdaUpdate()
                .eq(TeamEntity::getId, teamId)
                .eq(TeamEntity::getStatus, TeamStatus.ACTIVE.name())
                .set(TeamEntity::getStatus, TeamStatus.ARCHIVED.name())) == 1;
    }

    @Override
    public void insertTeamMember(long membershipId, long teamId, long userId, TeamRole role) {
        TeamMemberEntity entity = new TeamMemberEntity();
        entity.setId(membershipId);
        entity.setTeamId(teamId);
        entity.setUserId(userId);
        entity.setTeamRole(role.name());
        teamMemberMapper.insert(entity);
    }

    @Override
    public boolean updateTeamMemberRole(long teamId, long userId, TeamRole role) {
        return teamMemberMapper.update(Wrappers.<TeamMemberEntity>lambdaUpdate()
                .eq(TeamMemberEntity::getTeamId, teamId)
                .eq(TeamMemberEntity::getUserId, userId)
                .set(TeamMemberEntity::getTeamRole, role.name())) == 1;
    }

    @Override
    public boolean deleteTeamMember(long teamId, long userId) {
        return teamMemberMapper.delete(Wrappers.<TeamMemberEntity>lambdaQuery()
                .eq(TeamMemberEntity::getTeamId, teamId)
                .eq(TeamMemberEntity::getUserId, userId)) == 1;
    }

    @Override
    public Optional<TeamRole> findTeamRole(long teamId, long userId) {
        TeamMemberEntity entity = teamMemberMapper.selectOne(Wrappers.<TeamMemberEntity>lambdaQuery()
                .select(TeamMemberEntity::getTeamRole)
                .eq(TeamMemberEntity::getTeamId, teamId)
                .eq(TeamMemberEntity::getUserId, userId));
        return Optional.ofNullable(entity)
                .map(TeamMemberEntity::getTeamRole)
                .map(TeamRole::valueOf);
    }

    @Override
    public List<TeamAccess> findTeamAccesses(long userId) {
        List<TeamMemberEntity> memberships = teamMemberMapper.selectList(
                Wrappers.<TeamMemberEntity>lambdaQuery().eq(TeamMemberEntity::getUserId, userId)
        );
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<Long, Team> teams = teamMapper.selectBatchIds(
                        memberships.stream().map(TeamMemberEntity::getTeamId).toList()
                ).stream()
                .map(MybatisIdentityRepository::toDomain)
                .filter(team -> team.status() == TeamStatus.ACTIVE)
                .collect(Collectors.toMap(Team::id, Function.identity()));
        return memberships.stream()
                .filter(membership -> teams.containsKey(membership.getTeamId()))
                .map(membership -> new TeamAccess(
                        teams.get(membership.getTeamId()),
                        TeamRole.valueOf(membership.getTeamRole())
                ))
                .sorted(Comparator.comparing(access -> access.team().name()))
                .toList();
    }

    @Override
    public List<Team> findActiveTeams() {
        return teamMapper.selectList(Wrappers.<TeamEntity>lambdaQuery()
                        .eq(TeamEntity::getStatus, TeamStatus.ACTIVE.name())
                        .orderByAsc(TeamEntity::getName))
                .stream()
                .map(MybatisIdentityRepository::toDomain)
                .toList();
    }

    @Override
    public List<TeamMember> findTeamMembers(long teamId) {
        List<TeamMemberEntity> memberships = teamMemberMapper.selectList(
                Wrappers.<TeamMemberEntity>lambdaQuery().eq(TeamMemberEntity::getTeamId, teamId)
        );
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<Long, UserAccount> users = userMapper.selectBatchIds(
                        memberships.stream().map(TeamMemberEntity::getUserId).toList()
                ).stream()
                .map(MybatisIdentityRepository::toDomain)
                .collect(Collectors.toMap(UserAccount::id, Function.identity()));
        return memberships.stream()
                .filter(membership -> users.containsKey(membership.getUserId()))
                .map(membership -> new TeamMember(
                        users.get(membership.getUserId()),
                        TeamRole.valueOf(membership.getTeamRole())
                ))
                .sorted(Comparator.comparing(member -> member.user().displayName()))
                .toList();
    }

    private static UserEntity toEntity(UserAccount user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.id());
        entity.setEmail(user.email());
        entity.setDisplayName(user.displayName());
        entity.setPasswordHash(user.passwordHash());
        entity.setSystemRole(user.systemRole().name());
        entity.setStatus(user.status().name());
        return entity;
    }

    private static UserAccount toDomain(UserEntity entity) {
        return new UserAccount(
                entity.getId(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getPasswordHash(),
                SystemRole.valueOf(entity.getSystemRole()),
                UserStatus.valueOf(entity.getStatus())
        );
    }

    private static Team toDomain(TeamEntity entity) {
        return new Team(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getParentId(),
                entity.getCreatedBy(),
                TeamStatus.valueOf(entity.getStatus())
        );
    }
}
