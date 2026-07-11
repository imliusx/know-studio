package know.studio.arag.identity.domain;

import know.studio.arag.identity.api.WorkspaceRole;

import java.util.List;
import java.util.Optional;

public interface IdentityRepository {

    Optional<UserAccount> findUserById(long userId);

    Optional<UserAccount> findUserByEmail(String email);

    void insertUser(UserAccount user);

    void updateLastLogin(long userId);

    void insertWorkspace(Workspace workspace);

    void insertMember(long membershipId, long workspaceId, long userId, WorkspaceRole role);

    Optional<WorkspaceRole> findWorkspaceRole(long workspaceId, long userId);

    List<WorkspaceAccess> findWorkspaceAccesses(long userId);

    List<Workspace> findActiveWorkspaces();

    List<WorkspaceMember> findWorkspaceMembers(long workspaceId);
}
