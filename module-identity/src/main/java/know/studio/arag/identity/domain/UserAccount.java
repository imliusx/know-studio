package know.studio.arag.identity.domain;

import know.studio.arag.identity.api.SystemRole;

public record UserAccount(
        long id,
        String email,
        String displayName,
        String passwordHash,
        SystemRole systemRole,
        UserStatus status
) {
}
