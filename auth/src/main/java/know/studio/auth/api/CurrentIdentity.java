package know.studio.auth.api;

import know.studio.common.json.JsonLongId;

public record CurrentIdentity(
        @JsonLongId long userId,
        String email,
        String displayName,
        SystemRole systemRole
) {
}
