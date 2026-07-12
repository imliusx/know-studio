package know.studio.arag.identity.api;

import know.studio.arag.platform.core.json.JsonLongId;

public record CurrentIdentity(
        @JsonLongId long userId,
        String email,
        String displayName,
        SystemRole systemRole
) {
}
