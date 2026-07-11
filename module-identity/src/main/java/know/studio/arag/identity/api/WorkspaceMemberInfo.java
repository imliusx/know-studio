package know.studio.arag.identity.api;

public record WorkspaceMemberInfo(
        long userId,
        String email,
        String displayName,
        WorkspaceRole role
) {
}
