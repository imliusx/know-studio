package know.studio.arag.identity.api;

public record WorkspaceInfo(
        long workspaceId,
        String name,
        String description,
        long ownerId,
        WorkspaceRole role
) {
}
