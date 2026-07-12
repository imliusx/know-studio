package know.studio.arag.identity.api;

public record TeamMemberInfo(
        long userId,
        String email,
        String displayName,
        TeamRole role
) {
}
