package know.studio.arag.identity.api;

public interface IdentityApi {

    CurrentIdentity currentUser();

    WorkspaceRole requireWorkspaceReadable(long workspaceId);

    WorkspaceRole requireRole(long workspaceId, WorkspaceRole requiredRole);
}
