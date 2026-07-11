package know.studio.arag.identity.infra.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import know.studio.arag.identity.api.SystemRole;
import know.studio.arag.identity.api.WorkspaceRole;
import know.studio.arag.identity.domain.IdentityRepository;
import know.studio.arag.identity.domain.UserAccount;
import know.studio.arag.identity.domain.UserStatus;
import know.studio.arag.identity.domain.Workspace;
import know.studio.arag.identity.infra.persistence.entity.UserEntity;
import know.studio.arag.identity.infra.persistence.entity.WorkspaceEntity;
import know.studio.arag.identity.infra.persistence.entity.WorkspaceMemberEntity;
import know.studio.arag.identity.infra.persistence.mapper.UserMapper;
import know.studio.arag.identity.infra.persistence.mapper.WorkspaceMapper;
import know.studio.arag.identity.infra.persistence.mapper.WorkspaceMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MybatisIdentityRepository implements IdentityRepository {

    private final UserMapper userMapper;
    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceMemberMapper memberMapper;

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
    public void insertWorkspace(Workspace workspace) {
        WorkspaceEntity entity = new WorkspaceEntity();
        entity.setId(workspace.id());
        entity.setName(workspace.name());
        entity.setDescription(workspace.description());
        entity.setOwnerId(workspace.ownerId());
        entity.setStatus(workspace.status().name());
        workspaceMapper.insert(entity);
    }

    @Override
    public void insertMember(long membershipId, long workspaceId, long userId, WorkspaceRole role) {
        WorkspaceMemberEntity entity = new WorkspaceMemberEntity();
        entity.setId(membershipId);
        entity.setWorkspaceId(workspaceId);
        entity.setUserId(userId);
        entity.setWorkspaceRole(role.name());
        memberMapper.insert(entity);
    }

    @Override
    public Optional<WorkspaceRole> findWorkspaceRole(long workspaceId, long userId) {
        WorkspaceMemberEntity entity = memberMapper.selectOne(Wrappers.<WorkspaceMemberEntity>lambdaQuery()
                .select(WorkspaceMemberEntity::getWorkspaceRole)
                .eq(WorkspaceMemberEntity::getWorkspaceId, workspaceId)
                .eq(WorkspaceMemberEntity::getUserId, userId));
        return Optional.ofNullable(entity)
                .map(WorkspaceMemberEntity::getWorkspaceRole)
                .map(WorkspaceRole::valueOf);
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
}
