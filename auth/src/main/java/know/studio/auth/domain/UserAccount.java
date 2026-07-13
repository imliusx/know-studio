package know.studio.auth.domain;

import know.studio.auth.api.SystemRole;

public record UserAccount(
        long id,
        String email,
        String displayName,
        String passwordHash,
        SystemRole systemRole,
        UserStatus status
) {
}
