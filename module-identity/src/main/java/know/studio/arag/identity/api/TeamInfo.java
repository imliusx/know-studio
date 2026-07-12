package know.studio.arag.identity.api;

public record TeamInfo(
        long teamId,
        String name,
        String description,
        TeamRole role
) {
}
