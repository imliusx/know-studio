package know.studio.arag.identity.domain;

public record Workspace(
        long id,
        String name,
        String description,
        long ownerId,
        WorkspaceStatus status
) {
}
