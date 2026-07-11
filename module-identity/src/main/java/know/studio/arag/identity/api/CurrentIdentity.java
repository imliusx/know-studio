package know.studio.arag.identity.api;

public record CurrentIdentity(
        long userId,
        String email,
        String displayName,
        SystemRole systemRole
) {
}
